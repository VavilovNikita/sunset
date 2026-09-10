package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TableMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
import com.sunsetbeach.model.SpaAppointmentScheduleInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.SpaTherapist;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking, cancelling/completing/no-showing, and reading the half-hour grid for spa treatments.
 * See V41__spa_appointment.sql for the double-booking guard (a GiST exclusion constraint on each
 * of tableId/therapistUserId) - this service never checks availability itself before writing;
 * the database is the one source of truth for "is this table/therapist actually free", the same
 * "let the database settle it" philosophy CLAUDE.md's Concurrency section already documents for
 * Payment/Shift, extended here to overlapping time ranges.
 */
@Service
public class SpaAppointmentService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final SpaAppointmentRepository spaAppointmentRepository;
    private final BookingRepository bookingRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableMapper tableMapper;
    private final AuditLogService auditLogService;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final int slotMinutes;

    public SpaAppointmentService(
            SpaAppointmentRepository spaAppointmentRepository,
            BookingRepository bookingRepository,
            TableRepository tableRepository,
            UserRepository userRepository,
            MenuItemRepository menuItemRepository,
            TableMapper tableMapper,
            AuditLogService auditLogService,
            org.springframework.core.env.Environment env) {
        this.spaAppointmentRepository = spaAppointmentRepository;
        this.bookingRepository = bookingRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableMapper = tableMapper;
        this.auditLogService = auditLogService;
        this.openingTime = LocalTime.parse(env.getProperty("app.spa.opening-time", "09:00"));
        this.closingTime = LocalTime.parse(env.getProperty("app.spa.closing-time", "20:00"));
        this.slotMinutes = Integer.parseInt(env.getProperty("app.spa.slot-minutes", "30"));
    }

    @Transactional(readOnly = true)
    public SpaSchedule getSchedule(LocalDate date) {
        List<Table> tables = tableRepository.findByZone(Zone.SPA).stream()
                .filter(TableEntity::isActive)
                .map(tableMapper::toDto)
                .toList();
        List<SpaAppointment> appointments = spaAppointmentRepository.findByDate(date).stream().map(this::toDto).toList();
        return new SpaSchedule(date.toString(), openingTime.format(TIME_FORMAT), closingTime.format(TIME_FORMAT), slotMinutes, tables, appointments);
    }

    @Transactional(readOnly = true)
    public List<SpaTherapist> listTherapists() {
        return userRepository.findAll().stream()
                .filter(UserEntity::isActive)
                .filter(u -> Arrays.asList(u.getJobFunctions()).contains(JobFunction.THERAPIST.getValue()))
                .map(u -> new SpaTherapist(u.getId(), u.getEmail()))
                .toList();
    }

    @Transactional
    public SpaAppointmentResult create(SpaAppointmentCreateInput input, String actorUserId) {
        BookingEntity booking = bookingRepository.findById(input.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));
        TableEntity table = tableRepository.findById(input.getTableId()).orElseThrow(() -> new NotFoundException("Table not found"));
        UserEntity therapist = userRepository.findById(input.getTherapistUserId()).orElseThrow(() -> new NotFoundException("Therapist not found"));
        MenuItemEntity treatment =
                menuItemRepository.findById(input.getTreatmentMenuItemId()).orElseThrow(() -> new NotFoundException("Treatment not found"));

        if (treatment.getDepartment() != MenuDepartment.SPA || treatment.getDurationMinutes() == null) {
            throw new BadRequestException("treatmentMenuItemId must reference a SPA-department item with a duration set");
        }

        LocalDate date = LocalDate.parse(input.getDate());
        LocalTime startTime = LocalTime.parse(input.getStartTime());
        int durationMinutes = treatment.getDurationMinutes();
        validateWithinOpeningHours(startTime, durationMinutes);

        SpaAppointmentEntity entity = new SpaAppointmentEntity();
        entity.setBookingId(booking.getId());
        entity.setTableId(table.getId());
        entity.setTherapistUserId(therapist.getId());
        entity.setTreatmentMenuItemId(treatment.getId());
        entity.setDate(date);
        entity.setStartTime(startTime);
        entity.setDurationMinutes(durationMinutes);
        entity.setCreatedByUserId(actorUserId);

        SpaAppointmentEntity saved;
        try {
            saved = spaAppointmentRepository.saveAndFlush(entity);
        } catch (DataAccessException e) {
            throw translateOverlap(e);
        }

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_CREATED,
                AuditEntityType.SPA_APPOINTMENT,
                saved.getId(),
                "Booked " + treatment.getName() + " for " + booking.getGuestName() + " on " + date + " " + startTime.format(TIME_FORMAT) + " ("
                        + table.getLabel() + ", " + therapist.getEmail() + ")");

        // Inclusive both ends - the guest is still in the hotel on the departure day (see
        // CORRECTION 1: this is not the same [checkIn, checkOut) convention room occupancy uses,
        // which is about when the *room* is free for the next guest, not where the guest is).
        String warning = date.isBefore(booking.getCheckIn()) || date.isAfter(booking.getCheckOut())
                ? "This date falls outside " + booking.getGuestName() + "'s stay (" + booking.getCheckIn() + " to " + booking.getCheckOut() + ")."
                : null;

        return new SpaAppointmentResult(toDto(saved), warning);
    }

    @Transactional
    public SpaAppointment updateStatus(String id, SpaAppointmentStatusUpdateInput input, String actorUserId) {
        SpaAppointmentEntity entity = spaAppointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found"));
        SpaAppointmentStatus newStatus = input.getStatus();
        if (newStatus == SpaAppointmentStatus.BOOKED) {
            throw new BadRequestException("status must be COMPLETED, CANCELLED, or NO_SHOW");
        }
        if (entity.getStatus() != SpaAppointmentStatus.BOOKED) {
            throw new BadRequestException("This appointment is already " + entity.getStatus().getValue().toLowerCase());
        }

        entity.setStatus(newStatus);
        if (newStatus == SpaAppointmentStatus.CANCELLED) {
            entity.setCancelledByUserId(actorUserId);
            entity.setCancelReason(input.getCancelReason());
        }
        SpaAppointmentEntity saved = spaAppointmentRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_STATUS_CHANGED,
                AuditEntityType.SPA_APPOINTMENT,
                saved.getId(),
                "Spa appointment " + saved.getId() + " marked " + newStatus.getValue().toLowerCase());

        return toDto(saved);
    }

    /**
     * Moves an appointment to another table/time/therapist - the write path behind
     * {@code PATCH /spa-appointments/{id}/schedule}, the operation a drag on the spa grid needs.
     * Full replacement of all four fields, not a partial update: a drag always knows where it's
     * dropping, so there's no "unspecified" case to preserve the old value for (see
     * {@code SpaAppointmentScheduleInput}'s own description). Only legal while {@code status} is
     * {@code BOOKED} - the same lifecycle rule {@link #updateStatus} already enforces elsewhere;
     * an appointment that already ran or was cancelled isn't "moved," it's a new booking.
     * {@code durationMinutes} is untouched - the treatment's length doesn't change just because
     * it moved, only where/when it happens. Same "let the database settle it" philosophy as
     * {@link #create} - no conflict pre-check, the two GiST exclusion constraints
     * (V41/V43) fire on this UPDATE exactly as they do on that INSERT, translated the same way.
     */
    @Transactional
    public SpaAppointment updateSchedule(String id, SpaAppointmentScheduleInput input, String actorUserId) {
        SpaAppointmentEntity entity = spaAppointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (entity.getStatus() != SpaAppointmentStatus.BOOKED) {
            throw new BadRequestException("This appointment is already " + entity.getStatus().getValue().toLowerCase() + " - only a BOOKED appointment can be rescheduled");
        }
        TableEntity table = tableRepository.findById(input.getTableId()).orElseThrow(() -> new NotFoundException("Table not found"));
        UserEntity therapist = userRepository.findById(input.getTherapistUserId()).orElseThrow(() -> new NotFoundException("Therapist not found"));
        LocalDate date = LocalDate.parse(input.getDate());
        LocalTime startTime = LocalTime.parse(input.getStartTime());
        validateWithinOpeningHours(startTime, entity.getDurationMinutes());

        entity.setTableId(table.getId());
        entity.setTherapistUserId(therapist.getId());
        entity.setDate(date);
        entity.setStartTime(startTime);

        SpaAppointmentEntity saved;
        try {
            saved = spaAppointmentRepository.saveAndFlush(entity);
        } catch (DataAccessException e) {
            throw translateOverlap(e);
        }

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_RESCHEDULED,
                AuditEntityType.SPA_APPOINTMENT,
                saved.getId(),
                "Rescheduled to " + date + " " + startTime.format(TIME_FORMAT) + " (" + table.getLabel() + ", " + therapist.getEmail() + ")");

        return toDto(saved);
    }

    /**
     * {@code startTime} must land on a grid boundary and the whole treatment must fit before
     * closing - checked here (not left to the exclusion constraint, which only ever compares
     * appointments against each other, never against opening hours).
     */
    private void validateWithinOpeningHours(LocalTime startTime, int durationMinutes) {
        if (startTime.isBefore(openingTime)) {
            throw new BadRequestException("startTime is before the spa opens (" + openingTime.format(TIME_FORMAT) + ")");
        }
        long minutesSinceOpening = Duration.between(openingTime, startTime).toMinutes();
        if (minutesSinceOpening % slotMinutes != 0) {
            throw new BadRequestException("startTime must fall on a " + slotMinutes + "-minute slot boundary");
        }
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        if (!endTime.isAfter(startTime) || endTime.isAfter(closingTime)) {
            throw new BadRequestException("This treatment would run past closing (" + closingTime.format(TIME_FORMAT) + ")");
        }
    }

    /**
     * Distinguishes which axis lost the race by the exclusion constraint's own name - see
     * V41__spa_appointment.sql. Rethrows unrecognized failures untranslated rather than masking
     * them as a generic conflict.
     */
    private static ConflictException translateOverlap(DataAccessException e) {
        String message = rootCauseMessage(e);
        if (message != null && message.contains("spa_appointment_no_table_overlap")) {
            return new ConflictException("This table already has an appointment overlapping this time.");
        }
        if (message != null && message.contains("spa_appointment_no_therapist_overlap")) {
            return new ConflictException("This therapist already has an appointment overlapping this time.");
        }
        throw e;
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private SpaAppointment toDto(SpaAppointmentEntity e) {
        BookingEntity booking = bookingRepository.findById(e.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));
        TableEntity table = tableRepository.findById(e.getTableId()).orElseThrow(() -> new NotFoundException("Table not found"));
        UserEntity therapist = userRepository.findById(e.getTherapistUserId()).orElseThrow(() -> new NotFoundException("Therapist not found"));
        MenuItemEntity treatment =
                menuItemRepository.findById(e.getTreatmentMenuItemId()).orElseThrow(() -> new NotFoundException("Treatment not found"));
        return new SpaAppointment(
                e.getId(),
                e.getBookingId(),
                booking.getGuestName(),
                e.getTableId(),
                table.getLabel(),
                e.getTherapistUserId(),
                therapist.getEmail(),
                e.getTreatmentMenuItemId(),
                treatment.getName(),
                e.getDate().toString(),
                e.getStartTime().format(TIME_FORMAT),
                e.getDurationMinutes(),
                e.getStatus(),
                e.getOrderId(),
                e.getCreatedByUserId(),
                e.getCancelledByUserId(),
                e.getCancelReason(),
                TimestampFormat.toUtc(e.getCreatedAt()),
                TimestampFormat.toUtc(e.getUpdatedAt()));
    }
}
