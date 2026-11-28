package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.BookingMapper;
import com.sunsetbeach.mapper.DateText;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CheckInResult;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.CheckOutInput;
import com.sunsetbeach.model.CheckOutPreview;
import com.sunsetbeach.model.CheckOutResult;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RoomStillHeld;
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.model.TodayBoardEntry;
import com.sunsetbeach.model.TodayBoardRoomOccupant;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Physical guest occupancy - check-in, check-out, no-show, and the front desk's daily "today"
 * board - kept as its own service rather than growing {@link BookingService} further, mirroring
 * how {@link BookingCalendarService}/{@link AvailabilityService} already split their own read
 * models out.
 *
 * <p>Occupancy itself never touches {@link BookingSegmentEntity} or availability (the one
 * exception is an early checkout that also shortens the stay - see {@link #checkOut}): occupancy is a single value on the booking row ({@code
 * BookingEntity.occupancyStatus}), independent of how many room-change segments a stay has (a
 * relocation mid-stay never touches it), and never feeds back into what {@link
 * AvailabilityService}/{@link BookingCalendarService} compute - see the generated {@code
 * OccupancyStatus} schema's own description for why that separation is load-bearing, not
 * incidental.
 *
 * <p>{@code NO_SHOW} is a label, not an action: {@link #markNoShow} changes nothing about the
 * booking's dates, {@code status}, or availability. The deliberate way to actually release a
 * no-show's remaining nights is the existing cancel/shorten path - a separate decision by staff,
 * never a side effect of this one.
 */
@Service
public class BookingOccupancyService {

    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final GuestRepository guestRepository;
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;
    private final AuditLogService auditLogService;
    private final BookingWriter bookingWriter;
    private final GymReservationService gymReservations;
    private final OverstayChargeService overstayChargeService;
    private final OverstayRule overstayRule;
    private final BookingFolioCalculator folioCalculator;
    private final TransactionTemplate checkOutTransaction;
    private final Clock clock;

    public BookingOccupancyService(
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            GuestRepository guestRepository,
            BookingMapper bookingMapper,
            BookingService bookingService,
            AuditLogService auditLogService,
            BookingWriter bookingWriter,
            GymReservationService gymReservations,
            OverstayChargeService overstayChargeService,
            OverstayRule overstayRule,
            BookingFolioCalculator folioCalculator,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.gymReservations = gymReservations;
        this.overstayChargeService = overstayChargeService;
        this.overstayRule = overstayRule;
        this.folioCalculator = folioCalculator;
        this.bookingWriter = bookingWriter;
        this.checkOutTransaction = new TransactionTemplate(transactionManager);
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.guestRepository = guestRepository;
        this.bookingMapper = bookingMapper;
        this.bookingService = bookingService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * Only legal for a booking that is not {@code CANCELLED}, from its check-in date through its
     * check-out date (hotel date; {@link BookingMapper#checkInClosedReason}), once the part of the stay the guest sleeps
     * in today has a physical room ({@link OverstayRule#unitOn} - the moment a key is
     * handed over) and while occupancy is {@code EXPECTED} or {@code NO_SHOW} (a guest who
     * no-showed can still turn up late and check in normally). Anything else is a 409 (400 for the
     * missing room). Checking into a {@code DIRTY} room is a warning, not a rejection - a room
     * sometimes gets finished while the guest waits, and the desk must not stall on it.
     */
    @Transactional
    public CheckInResult checkIn(String bookingId) {
        BookingEntity booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("This booking is cancelled and can't be checked in.");
        }
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(booking.getCheckIn())) {
            throw new ConflictException("This booking can't be checked in before its check-in date (" + DateText.date(booking.getCheckIn()) + ").");
        }
        // A stay that is over is not checked into: the guest would be overdue from the first minute, with the nights
        // charged at the price list instead of the agreed price.
        BookingMapper.checkInClosedReason(booking, today).ifPresent(reason -> {
            throw new ConflictException(reason);
        });
        if (booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN) {
            throw new ConflictException("This booking is already checked in.");
        }
        if (booking.getOccupancyStatus() == OccupancyStatus.CHECKED_OUT) {
            throw new ConflictException("This booking has already checked out.");
        }

        // The room the guest walks into today, and the only one check-in needs: a move planned for a later night may
        // still have none.
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        String arrivalUnitId = OverstayRule.unitOn(booking, segments, today, clock);
        if (arrivalUnitId == null) {
            throw new BadRequestException(BookingMapper.roomNeededReason(booking, segments, today));
        }
        // Locked, so two arrivals assigned to one room can't both find it empty.
        RoomUnitEntity unit =
                roomUnitRepository.findByIdForUpdate(arrivalUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
        // A block, not a warning like a dirty room: who is checked into a room is the system's own record, and two
        // guests in one room break check-out, housekeeping, the maps and charges to the room.
        overstayRule.otherGuestIn(unit.getId(), bookingId).ifPresent(other -> {
            throw new ConflictException(
                    OverstayRule.occupiedMessage(unit.getLabel(), other, "Check them out or move this arrival to another room first."));
        });
        // So a move of another guest into this room that is running right now fails and retries (BookingWriter).
        roomUnitRepository.markUsed(unit.getId());
        boolean wasDirty = unit.getHousekeepingStatus() == HousekeepingStatus.DIRTY;

        booking.setOccupancyStatus(OccupancyStatus.CHECKED_IN);
        booking.setCheckedInAt(nowUtc());
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        auditLogService.record(
                AuditAction.BOOKING_CHECKED_IN,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Checked in " + saved.getGuestName() + " (" + unit.getLabel() + ")" + (wasDirty ? " - room was not marked clean" : ""));

        String warning = wasDirty ? "Room " + unit.getLabel() + " has not been marked clean since the last checkout." : null;
        return new CheckInResult(toDto(saved), warning);
    }

    /**
     * Only legal while occupancy is {@code CHECKED_IN}. Sets the booking's current room {@code
     * DIRTY} ({@code PATCH /room-units/{id}/housekeeping} marks it cleaned again - a separate,
     * deliberate step) and reports {@link BookingService#computeOutstandingBalance} at this
     * instant, the last moment front desk can act on it before the guest walks out.
     *
     * <p>An early checkout with {@code shortenStay} first moves {@code checkOut} to today (never
     * before the first night) through {@link BookingService#updateSchedule} - the same path a
     * desk-side schedule change takes, so availability, the calendar, the night audit and every
     * report see the shorter stay without knowing about checkout. Leaving the old dates standing
     * kept the room sold and the nights counted after the guest had gone. With {@code
     * chargeUnusedNights} the released nights' agreed price (the drop in {@code totalPrice}) is
     * kept as {@code earlyDepartureFee}. Not one transaction: the schedule change runs
     * SERIALIZABLE in {@link BookingWriter}, which can't join an outer transaction, so the
     * preconditions are checked before it and the checkout itself follows in its own.
     */
    public CheckOutResult checkOut(String bookingId, CheckOutInput input) {
        boolean shorten = input != null && Boolean.TRUE.equals(input.getShortenStay());
        boolean chargeUnused = input != null && Boolean.TRUE.equals(input.getChargeUnusedNights());
        BookingEntity before = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        requireCheckedIn(before);

        BigDecimal released = BigDecimal.ZERO;
        // Read now: inside a caller's transaction `before` is the same managed entity the schedule
        // change below rewrites.
        LocalDate originalCheckOut = before.getCheckOut();
        BigDecimal priceBefore = before.getTotalPrice();
        // The room the guest is physically in now, read before an early departure drops trailing segments
        // (a move that took effect today puts them in the new room, which the shortened stay forgets).
        String currentRoomUnitId = currentRoomUnitId(before);
        if (shorten) {
            EarlyDeparture plan = planEarlyDeparture(before);
            if (plan != null) {
                if (!plan.quote().available()) {
                    throw new ConflictException(plan.quote().reason());
                }
                BookingScheduleInput schedule = new BookingScheduleInput(before.getCheckIn().toString(), plan.shortenedCheckOut().toString())
                        .roomUnitId(plan.roomUnitId());
                Booking shortened = bookingService.updateSchedule(bookingId, schedule, true);
                released = priceBefore.subtract(new BigDecimal(shortened.getTotalPrice()));
            }
        }
        BigDecimal releasedAmount = released;
        BigDecimal fee = chargeUnused ? released : BigDecimal.ZERO;
        return checkOutTransaction.execute(status -> completeCheckOut(bookingId, originalCheckOut, releasedAmount, fee, currentRoomUnitId));
    }

    /** {@link OverstayRule#unitOn} for a booking's stored segments, today. */
    private String currentRoomUnitId(BookingEntity booking) {
        return OverstayRule.unitOn(booking, segmentRepository.findByBookingIdOrderByCheckInAsc(booking.getId()), LocalDate.now(clock), clock);
    }

    private CheckOutResult completeCheckOut(
            String bookingId, LocalDate originalCheckOut, BigDecimal released, BigDecimal fee, String currentRoomUnitId) {
        BookingEntity booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        requireCheckedIn(booking);
        // The nights slept past check-out are charged up to last night; tonight's, accrued at the start of the day, is released.
        overstayChargeService.settleAtCheckOut(booking);

        booking.setOccupancyStatus(OccupancyStatus.CHECKED_OUT);
        booking.setCheckedOutAt(nowUtc());
        if (fee.signum() > 0) {
            booking.setEarlyDepartureFee(booking.getEarlyDepartureFee().add(fee));
        }
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        // The guest has left: gym sessions still ahead (or running) go with them, in this same transaction.
        int gymCancelled = gymReservations.onBookingEnded(saved, "Guest checked out");

        String roomLabel = null;
        if (currentRoomUnitId != null) {
            RoomUnitEntity unit =
                    roomUnitRepository.findById(currentRoomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            unit.setHousekeepingStatus(HousekeepingStatus.DIRTY);
            roomUnitRepository.saveAndFlush(unit);
            roomLabel = unit.getLabel();
        }
        BigDecimal outstanding = bookingService.computeOutstandingBalance(bookingId);

        String shortened = "";
        if (!saved.getCheckOut().equals(originalCheckOut)) {
            shortened = "; stay shortened from " + DateText.date(originalCheckOut) + " to " + DateText.date(saved.getCheckOut())
                    + (fee.signum() > 0
                            ? ", unused nights (" + PriceFormat.baht(fee) + ") charged as early departure"
                            : ", unused nights (" + PriceFormat.baht(released) + ") not charged");
        }
        auditLogService.record(
                AuditAction.BOOKING_CHECKED_OUT,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Checked out " + saved.getGuestName() + (roomLabel != null ? " (" + roomLabel + ", now marked dirty)" : "")
                        + shortened
                        + (gymCancelled > 0 ? "; " + gymCancelled + " gym reservation(s) cancelled" : "")
                        + (outstanding.signum() > 0
                                ? "; checkout confirmed with " + PriceFormat.baht(outstanding) + " unpaid"
                                : ""));

        return new CheckOutResult(toDto(saved), PriceFormat.asDecimalString(outstanding));
    }

    /**
     * {@code GET /bookings/{id}/check-out/preview} - what {@link #checkOut} with {@code
     * shortenStay} would do right now, priced by the same {@link BookingWriter#quoteSchedule} the
     * schedule form uses.
     */
    @Transactional(readOnly = true)
    public CheckOutPreview previewCheckOut(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BigDecimal currentRoom = booking.roomAmount();
        String currentTotal = PriceFormat.asDecimalString(currentRoom);
        // Tonight's overstay night is released by the check-out itself, so it is not part of what the guest settles now.
        // Both figures are answered, so the screen can say why it is not the one on the Today board.
        BigDecimal dueNow = bookingService.computeOutstandingBalance(bookingId);
        BigDecimal dueAfter = dueNow.subtract(overstayChargeService.tonightsCharge(bookingId)).max(BigDecimal.ZERO);
        String outstanding = PriceFormat.asDecimalString(dueAfter);
        String owedNow = PriceFormat.asDecimalString(dueNow);
        boolean tonightNotCharged = dueNow.compareTo(dueAfter) > 0;
        EarlyDeparture plan = planEarlyDeparture(booking);
        if (plan == null) {
            return new CheckOutPreview(false, false, null, null, 0, "0.00", currentTotal, currentTotal, outstanding, owedNow, tonightNotCharged, outstanding);
        }
        int nightsReleased = (int) ChronoUnit.DAYS.between(plan.shortenedCheckOut(), booking.getCheckOut());
        if (!plan.quote().available()) {
            return new CheckOutPreview(
                    true, false, plan.quote().reason(), plan.shortenedCheckOut(), nightsReleased, "0.00", currentTotal, currentTotal, outstanding,
                    owedNow, tonightNotCharged, outstanding);
        }
        BigDecimal unused = booking.getTotalPrice().subtract(plan.quote().totalPrice());
        BigDecimal shortenedOutstanding = bookingService.computeOutstandingBalanceWithStayPrice(bookingId, plan.quote().totalPrice());
        return new CheckOutPreview(
                true,
                true,
                null,
                plan.shortenedCheckOut(),
                nightsReleased,
                PriceFormat.asDecimalString(unused),
                currentTotal,
                PriceFormat.asDecimalString(currentRoom.subtract(unused)),
                outstanding,
                owedNow,
                tonightNotCharged,
                PriceFormat.asDecimalString(shortenedOutstanding));
    }

    /**
     * Null when checking out today isn't early: the stay already ends today or earlier, or it's a
     * one-night stay leaving on its arrival day (the first night is always charged). The room
     * passed to the schedule change is the one holding the stay's new last night - the one the
     * guest is leaving. After a relocation that is not always the last segment's: a guest who
     * leaves on the day of the move never uses the new room, and its segment is dropped.
     */
    private EarlyDeparture planEarlyDeparture(BookingEntity booking) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstNightEnd = booking.getCheckIn().plusDays(1);
        LocalDate shortened = today.isAfter(firstNightEnd) ? today : firstNightEnd;
        if (!shortened.isBefore(booking.getCheckOut())) {
            return null;
        }
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(booking.getId());
        BookingSegmentEntity leaving = segments.stream().filter(s -> s.getCheckIn().isBefore(shortened)).reduce((first, second) -> second).orElse(null);
        String roomUnitId = leaving != null ? leaving.getRoomUnitId() : booking.getRoomUnitId();
        BookingWriter.ScheduleQuote quote = bookingWriter.quoteSchedule(booking.getId(), booking.getCheckIn(), shortened, roomUnitId, true);
        return new EarlyDeparture(shortened, roomUnitId, quote);
    }

    private record EarlyDeparture(LocalDate shortenedCheckOut, String roomUnitId, BookingWriter.ScheduleQuote quote) {}

    private static void requireCheckedIn(BookingEntity booking) {
        if (booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
            throw new ConflictException("This booking hasn't been checked in.");
        }
    }

    /**
     * Only legal while occupancy is {@code EXPECTED} - a booking already checked in, checked
     * out, or already marked no-show has nothing left for this to change.
     */
    @Transactional
    public Booking markNoShow(String bookingId) {
        // Locked like checkIn/updateStatus: the save writes the whole row, and a stale read would
        // put back a status another request had just changed (audit finding H5).
        BookingEntity booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getOccupancyStatus() != OccupancyStatus.EXPECTED) {
            throw new ConflictException("This booking is not awaiting arrival.");
        }

        booking.setOccupancyStatus(OccupancyStatus.NO_SHOW);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        auditLogService.record(
                AuditAction.BOOKING_NO_SHOW_MARKED, AuditEntityType.BOOKING, saved.getId(), "Marked " + saved.getGuestName() + " as no-show");

        return toDto(saved);
    }

    /**
     * The front desk's daily working set - see the generated {@code TodayBoard} schema's own
     * description for exactly which bookings land in which of the three lists. Computed fresh on
     * every call, not a stored snapshot. "Today" is the hotel's date from the shared {@link
     * Clock}: a bare {@code LocalDate.now()} on the UTC server showed yesterday's board until
     * 07:00 Bangkok.
     */
    @Transactional(readOnly = true)
    public TodayBoard getTodayBoard() {
        LocalDate today = LocalDate.now(clock);
        Map<String, List<OverstayRule.InHouseGuest>> inHouseByUnit = overstayRule.inHouseByRoomUnit();
        List<TodayBoardEntry> arriving = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckInIs(OccupancyStatus.EXPECTED, BookingStatus.CANCELLED, today)
                .stream()
                .map(b -> toEntry(b, today, inHouseByUnit).roomOccupant(roomOccupant(inHouseByUnit, b)))
                .toList();
        // On or before today, not only on it: a guest who should have left on an earlier day is
        // still due out (OverstayRule), and this list is where the desk's check-out button lives.
        List<TodayBoardEntry> departing = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckOutLessThanEqual(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED, today)
                .stream()
                .map(b -> toEntry(b, today, inHouseByUnit))
                .toList();
        List<TodayBoardEntry> inHouse = bookingRepository
                .findByOccupancyStatusAndStatusNot(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED)
                .stream()
                .map(b -> toEntry(b, today, inHouseByUnit))
                .toList();
        return new TodayBoard(arriving, departing, inHouse);
    }

    /** The guest still in the room an arrival would check into, or null - {@link #checkIn}'s own question. */
    private TodayBoardRoomOccupant roomOccupant(Map<String, List<OverstayRule.InHouseGuest>> inHouseByUnit, BookingEntity arrival) {
        String unitId = currentRoomUnitId(arrival);
        if (unitId == null) {
            return null;
        }
        return OverstayRule.otherGuestIn(inHouseByUnit, unitId, arrival.getId())
                .map(g -> new TodayBoardRoomOccupant(g.booking().getId(), g.booking().getGuestName(), g.overdueDays()).awaitingRoom(g.awaitingRoom()))
                .orElse(null);
    }

    /**
     * {@code checkedInAt}/{@code checkedOutAt} are UTC wall-clock, like the {@code
     * @CreationTimestamp}/{@code @UpdateTimestamp} columns beside them: {@code BookingMapper} labels
     * them {@code Z} via {@code TimestampFormat.toUtc}, and {@code ReportService}'s in-house test
     * compares {@code checkedOutAt} against {@link ReportDateRange}'s UTC bounds. The shared clock is
     * Bangkok-zoned, so {@code LocalDateTime.now(clock)} would store the hotel's wall-clock, seven
     * hours ahead of what every reader assumes - take the clock's instant, rendered in UTC.
     */
    private LocalDateTime nowUtc() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    private TodayBoardEntry toEntry(BookingEntity entity, LocalDate today, Map<String, List<OverstayRule.InHouseGuest>> inHouseByUnit) {
        BookingFolioCalculator.FolioBreakdown folio = folioCalculator.breakdown(entity);
        Booking dto = toDto(entity);
        return new TodayBoardEntry(dto, PriceFormat.asDecimalString(folio.balanceDue()))
                .pendingRoomMove(dto.getPendingRoomMove())
                .roomStillHeld(roomStillHeld(inHouseByUnit, entity.getId()))
                .overdueDays(OverstayRule.overdueDays(entity, today))
                // The one count to show next to the guest: the nights charged, tonight's among them or not.
                .overstayNights(folio.overstayNightsOwed())
                .overstayIncludesTonight(folio.overstayNightsOwed() > 0 && folio.overstayIncludesTonight());
    }

    private RoomStillHeld roomStillHeld(Map<String, List<OverstayRule.InHouseGuest>> inHouseByUnit, String bookingId) {
        return OverstayRule.roomStillHeld(inHouseByUnit, bookingId)
                .map(held -> BookingMapper.roomStillHeld(
                        held, roomUnitRepository.findById(held.roomUnitId()).map(RoomUnitEntity::getLabel).orElse("")))
                .orElse(null);
    }

    private Booking toDto(BookingEntity entity) {
        RoomEntity room = roomRepository.findById(entity.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomUnitEntity roomUnit = entity.getRoomUnitId() != null ? roomUnitRepository.findById(entity.getRoomUnitId()).orElse(null) : null;
        GuestEntity guest = entity.getGuestId() != null ? guestRepository.findById(entity.getGuestId()).orElse(null) : null;
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(entity.getId());
        return bookingMapper.toDto(entity, room, roomUnit, guest, segments);
    }
}
