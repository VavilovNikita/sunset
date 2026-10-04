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
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.model.TodayBoardEntry;
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
            PlatformTransactionManager transactionManager,
            Clock clock) {
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
     * Only legal once a physical room is assigned ({@code booking.roomUnitId} - the moment a key
     * is handed over) and while occupancy is {@code EXPECTED} or {@code NO_SHOW} (a guest who
     * no-showed can still turn up late and check in normally). Checking into a {@code DIRTY}
     * room is a warning, not a rejection - a room sometimes gets finished while the guest waits,
     * and the desk must not stall on it.
     */
    @Transactional
    public CheckInResult checkIn(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getRoomUnitId() == null) {
            throw new BadRequestException("Assign a room before checking in.");
        }
        if (booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN) {
            throw new ConflictException("This booking is already checked in.");
        }
        if (booking.getOccupancyStatus() == OccupancyStatus.CHECKED_OUT) {
            throw new ConflictException("This booking has already checked out.");
        }

        RoomUnitEntity unit =
                roomUnitRepository.findById(booking.getRoomUnitId()).orElseThrow(() -> new NotFoundException("Room unit not found"));
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
        if (shorten) {
            EarlyDeparture plan = planEarlyDeparture(before);
            if (plan != null) {
                if (!plan.quote().available()) {
                    throw new ConflictException(plan.quote().reason());
                }
                BookingScheduleInput schedule = new BookingScheduleInput(before.getCheckIn().toString(), plan.shortenedCheckOut().toString())
                        .roomUnitId(plan.roomUnitId());
                Booking shortened = bookingService.updateSchedule(bookingId, schedule);
                released = priceBefore.subtract(new BigDecimal(shortened.getTotalPrice()));
            }
        }
        BigDecimal releasedAmount = released;
        BigDecimal fee = chargeUnused ? released : BigDecimal.ZERO;
        return checkOutTransaction.execute(status -> completeCheckOut(bookingId, originalCheckOut, releasedAmount, fee));
    }

    private CheckOutResult completeCheckOut(String bookingId, LocalDate originalCheckOut, BigDecimal released, BigDecimal fee) {
        BookingEntity booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        requireCheckedIn(booking);

        booking.setOccupancyStatus(OccupancyStatus.CHECKED_OUT);
        booking.setCheckedOutAt(nowUtc());
        if (fee.signum() > 0) {
            booking.setEarlyDepartureFee(booking.getEarlyDepartureFee().add(fee));
        }
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        String roomLabel = null;
        if (saved.getRoomUnitId() != null) {
            RoomUnitEntity unit =
                    roomUnitRepository.findById(saved.getRoomUnitId()).orElseThrow(() -> new NotFoundException("Room unit not found"));
            unit.setHousekeepingStatus(HousekeepingStatus.DIRTY);
            roomUnitRepository.saveAndFlush(unit);
            roomLabel = unit.getLabel();
        }

        BigDecimal outstanding = bookingService.computeOutstandingBalance(bookingId);

        String shortened = "";
        if (!saved.getCheckOut().equals(originalCheckOut)) {
            shortened = "; stay shortened from " + originalCheckOut + " to " + saved.getCheckOut()
                    + (fee.signum() > 0
                            ? ", unused nights (฿" + PriceFormat.asDecimalString(fee) + ") charged as early departure"
                            : ", unused nights (฿" + PriceFormat.asDecimalString(released) + ") not charged");
        }
        auditLogService.record(
                AuditAction.BOOKING_CHECKED_OUT,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Checked out " + saved.getGuestName() + (roomLabel != null ? " (" + roomLabel + ", now marked dirty)" : "") + shortened
                        + (outstanding.signum() > 0 ? "; ฿" + outstanding + " outstanding" : ""));

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
        String outstanding = PriceFormat.asDecimalString(bookingService.computeOutstandingBalance(bookingId));
        EarlyDeparture plan = planEarlyDeparture(booking);
        if (plan == null) {
            return new CheckOutPreview(false, false, null, null, 0, "0.00", currentTotal, currentTotal, outstanding);
        }
        int nightsReleased = (int) ChronoUnit.DAYS.between(plan.shortenedCheckOut(), booking.getCheckOut());
        if (!plan.quote().available()) {
            return new CheckOutPreview(
                    true, false, plan.quote().reason(), plan.shortenedCheckOut(), nightsReleased, "0.00", currentTotal, currentTotal, outstanding);
        }
        BigDecimal unused = booking.getTotalPrice().subtract(plan.quote().totalPrice());
        return new CheckOutPreview(
                true,
                true,
                null,
                plan.shortenedCheckOut(),
                nightsReleased,
                PriceFormat.asDecimalString(unused),
                currentTotal,
                PriceFormat.asDecimalString(currentRoom.subtract(unused)),
                outstanding);
    }

    /**
     * Null when checking out today isn't early: the stay already ends today or earlier, or it's a
     * one-night stay leaving on its arrival day (the first night is always charged). The room
     * passed to the schedule change is the last segment's - the one the guest is leaving.
     */
    private EarlyDeparture planEarlyDeparture(BookingEntity booking) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstNightEnd = booking.getCheckIn().plusDays(1);
        LocalDate shortened = today.isAfter(firstNightEnd) ? today : firstNightEnd;
        if (!shortened.isBefore(booking.getCheckOut())) {
            return null;
        }
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(booking.getId());
        String roomUnitId = segments.isEmpty() ? booking.getRoomUnitId() : segments.get(segments.size() - 1).getRoomUnitId();
        BookingWriter.ScheduleQuote quote = bookingWriter.quoteSchedule(booking.getId(), booking.getCheckIn(), shortened, roomUnitId);
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
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
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
        List<TodayBoardEntry> arriving = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckInIs(OccupancyStatus.EXPECTED, BookingStatus.CANCELLED, today)
                .stream()
                .map(b -> toEntry(b, today))
                .toList();
        // On or before today, not only on it: a guest who should have left on an earlier day is
        // still due out (OverstayRule), and this list is where the desk's check-out button lives.
        List<TodayBoardEntry> departing = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckOutLessThanEqual(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED, today)
                .stream()
                .map(b -> toEntry(b, today))
                .toList();
        List<TodayBoardEntry> inHouse = bookingRepository
                .findByOccupancyStatusAndStatusNot(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED)
                .stream()
                .map(b -> toEntry(b, today))
                .toList();
        return new TodayBoard(arriving, departing, inHouse);
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

    private TodayBoardEntry toEntry(BookingEntity entity, LocalDate today) {
        BigDecimal outstanding = bookingService.computeOutstandingBalance(entity.getId());
        return new TodayBoardEntry(toDto(entity), PriceFormat.asDecimalString(outstanding))
                .overdueDays(OverstayRule.overdueDays(entity, today));
    }

    private Booking toDto(BookingEntity entity) {
        RoomEntity room = roomRepository.findById(entity.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomUnitEntity roomUnit = entity.getRoomUnitId() != null ? roomUnitRepository.findById(entity.getRoomUnitId()).orElse(null) : null;
        GuestEntity guest = entity.getGuestId() != null ? guestRepository.findById(entity.getGuestId()).orElse(null) : null;
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(entity.getId());
        return bookingMapper.toDto(entity, room, roomUnit, guest, segments);
    }
}
