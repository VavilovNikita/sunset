package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.NightAuditEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.SqlStates;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.NightAudit;
import com.sunsetbeach.model.NightAuditBooking;
import com.sunsetbeach.model.NightAuditCloseInput;
import com.sunsetbeach.model.NightAuditClosure;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.NightAuditRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /night-audit} and {@code POST /night-audit/close} - a daily checklist and a receipt
 * that someone reviewed it, deliberately not a ledger process (see the NightAudit tag in
 * openapi.yaml). Nothing here acts on a booking or blocks anything: the lists surface bookings for
 * a person to decide about, and a closure records only that the review happened.
 */
@Service
public class NightAuditService {

    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    private final BookingRepository bookingRepository;
    private final NightAuditRepository nightAuditRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public NightAuditService(
            BookingRepository bookingRepository,
            NightAuditRepository nightAuditRepository,
            UserRepository userRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            ReportService reportService,
            AuditLogService auditLogService,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.nightAuditRepository = nightAuditRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.reportService = reportService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * The snapshot is {@link ReportService#occupancy}'s own {@code total} row for a one-night
     * range - called, never reimplemented, so it can't drift from {@code GET /reports/occupancy}.
     */
    @Transactional(readOnly = true)
    public NightAudit get(String date) {
        LocalDate day = date == null ? LocalDate.now(clock) : parseDate(date);
        String iso = day.toString();
        return new NightAudit(
                iso,
                toBookingDtos(missedArrivals(day)),
                toBookingDtos(missedDepartures(day)),
                reportService.occupancy(iso, iso).getTotal(),
                nightAuditRepository.findByDate(day).map(this::toClosureDto).orElse(null));
    }

    /**
     * One row per date, enforced by {@code NightAudit_date_key} rather than a check-then-write, so
     * two people closing the same date at once still get exactly one row and one 409.
     */
    @Transactional
    public NightAuditClosure close(NightAuditCloseInput input) {
        LocalDate day = parseDate(input.getDate());
        String notes = input.getNotes().isPresent() ? input.getNotes().get() : null;
        notes = notes == null || notes.isBlank() ? null : notes.trim();

        StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        NightAuditEntity entity = new NightAuditEntity();
        entity.setDate(day);
        entity.setClosedByUserId(actor.id());
        entity.setNotes(notes);
        NightAuditEntity saved;
        try {
            saved = nightAuditRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (SqlStates.is(e, UNIQUE_VIOLATION_SQLSTATE)) {
                throw new ConflictException(day + " is already closed.");
            }
            throw e;
        }

        // What was still open at close is worth keeping: closing with unresolved items is allowed,
        // and the audit entry is the only place that records it was a deliberate choice.
        auditLogService.record(
                AuditAction.NIGHT_AUDIT_CLOSED,
                AuditEntityType.NIGHT_AUDIT,
                saved.getId(),
                "Closed night audit for " + day + " (" + missedArrivals(day).size() + " missed arrival(s), "
                        + missedDepartures(day).size() + " missed departure(s) open)");
        return toClosureDto(saved);
    }

    private List<BookingEntity> missedArrivals(LocalDate day) {
        return bookingRepository.findByOccupancyStatusAndStatusNotAndCheckInLessThanEqualOrderByCheckInAsc(
                OccupancyStatus.EXPECTED, BookingStatus.CANCELLED, day);
    }

    private List<BookingEntity> missedDepartures(LocalDate day) {
        return bookingRepository.findByOccupancyStatusAndCheckOutLessThanEqualOrderByCheckOutAsc(OccupancyStatus.CHECKED_IN, day);
    }

    /** The openapi {@code pattern} only guarantees the shape - {@code 2026-02-30} passes it. Same as {@link ReportDateRange}. */
    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationException.field("date", "must be a valid date (YYYY-MM-DD)");
        }
    }

    /** Room type and unit names in two batched lookups, not a lazy load per booking. */
    private List<NightAuditBooking> toBookingDtos(List<BookingEntity> bookings) {
        Map<String, String> roomNames = roomRepository.findAllById(bookings.stream().map(BookingEntity::getRoomId).distinct().toList())
                .stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));
        Map<String, String> unitLabels = roomUnitRepository.findAllById(
                        bookings.stream().map(BookingEntity::getRoomUnitId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(RoomUnitEntity::getId, RoomUnitEntity::getLabel));
        return bookings.stream().map(b -> toBookingDto(b, roomNames.get(b.getRoomId()), unitLabels.get(b.getRoomUnitId()))).toList();
    }

    private static NightAuditBooking toBookingDto(BookingEntity booking, String roomName, String unitLabel) {
        return new NightAuditBooking(
                booking.getId(),
                booking.getGuestName(),
                roomName,
                unitLabel,
                booking.getCheckIn().toString(),
                booking.getCheckOut().toString(),
                booking.getStatus(),
                booking.getOccupancyStatus());
    }

    private NightAuditClosure toClosureDto(NightAuditEntity entity) {
        // FK on closedByUserId (V114), so the user row always exists.
        String closedByName = userRepository.findById(entity.getClosedByUserId()).map(UserEntity::getName).orElse(entity.getClosedByUserId());
        return new NightAuditClosure(
                entity.getDate().toString(),
                entity.getClosedByUserId(),
                closedByName,
                TimestampFormat.toUtc(entity.getClosedAt()),
                entity.getNotes());
    }
}
