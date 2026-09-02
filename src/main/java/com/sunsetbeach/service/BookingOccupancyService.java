package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
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
import com.sunsetbeach.model.CheckOutResult;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.model.TodayBoardEntry;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Physical guest occupancy - check-in, check-out, no-show, and the front desk's daily "today"
 * board - kept as its own service rather than growing {@link BookingService} further, mirroring
 * how {@link BookingCalendarService}/{@link AvailabilityService} already split their own read
 * models out.
 *
 * <p>Deliberately does not touch {@link BookingWriter}, {@link BookingSegmentEntity}, or
 * availability at all: occupancy is a single value on the booking row ({@code
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
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;
    private final AuditLogService auditLogService;

    public BookingOccupancyService(
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            BookingMapper bookingMapper,
            BookingService bookingService,
            AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.bookingMapper = bookingMapper;
        this.bookingService = bookingService;
        this.auditLogService = auditLogService;
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
        booking.setCheckedInAt(LocalDateTime.now());
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
     */
    @Transactional
    public CheckOutResult checkOut(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
            throw new ConflictException("This booking hasn't been checked in.");
        }

        booking.setOccupancyStatus(OccupancyStatus.CHECKED_OUT);
        booking.setCheckedOutAt(LocalDateTime.now());
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

        auditLogService.record(
                AuditAction.BOOKING_CHECKED_OUT,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Checked out " + saved.getGuestName() + (roomLabel != null ? " (" + roomLabel + ", now marked dirty)" : "")
                        + (outstanding.signum() > 0 ? "; ฿" + outstanding + " outstanding" : ""));

        return new CheckOutResult(toDto(saved), PriceFormat.asDecimalString(outstanding));
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
     * every call, not a stored snapshot.
     */
    @Transactional(readOnly = true)
    public TodayBoard getTodayBoard() {
        LocalDate today = LocalDate.now();
        List<TodayBoardEntry> arriving = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckInIs(OccupancyStatus.EXPECTED, BookingStatus.CANCELLED, today)
                .stream()
                .map(this::toEntry)
                .toList();
        List<TodayBoardEntry> departing = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckOut(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED, today)
                .stream()
                .map(this::toEntry)
                .toList();
        List<TodayBoardEntry> inHouse = bookingRepository
                .findByOccupancyStatusAndStatusNot(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED)
                .stream()
                .map(this::toEntry)
                .toList();
        return new TodayBoard(arriving, departing, inHouse);
    }

    private TodayBoardEntry toEntry(BookingEntity entity) {
        BigDecimal outstanding = bookingService.computeOutstandingBalance(entity.getId());
        return new TodayBoardEntry(toDto(entity), PriceFormat.asDecimalString(outstanding));
    }

    private Booking toDto(BookingEntity entity) {
        RoomEntity room = roomRepository.findById(entity.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomUnitEntity roomUnit = entity.getRoomUnitId() != null ? roomUnitRepository.findById(entity.getRoomUnitId()).orElse(null) : null;
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(entity.getId());
        return bookingMapper.toDto(entity, room, roomUnit, segments);
    }
}
