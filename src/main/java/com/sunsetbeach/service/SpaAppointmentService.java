package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.SpaAppointmentTreatmentEntity;
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
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
import com.sunsetbeach.model.SpaAppointmentScheduleInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaAppointmentTreatment;
import com.sunsetbeach.model.SpaAppointmentTreatmentCreateInput;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.SpaTherapist;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.SpaAppointmentTreatmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking, cancelling/completing/no-showing, and reading the half-hour grid for spa treatments,
 * plus adding/removing individual treatments on an already-booked appointment. See
 * V41__spa_appointment.sql for the double-booking guard (a GiST exclusion constraint on each of
 * tableId/therapistUserId) - this service never checks availability itself before writing; the
 * database is the one source of truth for "is this table/therapist actually free", the same
 * "let the database settle it" philosophy CLAUDE.md's Concurrency section already documents for
 * Payment/Shift, extended here to overlapping time ranges.
 *
 * <p>Treatments live in {@link SpaAppointmentTreatmentEntity}, one row each - see that entity's
 * javadoc and V50__spa_appointment_treatment.sql for why, and CLAUDE.md's Spa billing section
 * for why only duration freezes there, never price.
 */
@Service
public class SpaAppointmentService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final SpaAppointmentRepository spaAppointmentRepository;
    private final SpaAppointmentTreatmentRepository spaAppointmentTreatmentRepository;
    private final BookingRepository bookingRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableMapper tableMapper;
    private final AuditLogService auditLogService;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final int slotMinutes;

    public SpaAppointmentService(
            SpaAppointmentRepository spaAppointmentRepository,
            SpaAppointmentTreatmentRepository spaAppointmentTreatmentRepository,
            BookingRepository bookingRepository,
            TableRepository tableRepository,
            UserRepository userRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            TableMapper tableMapper,
            AuditLogService auditLogService,
            org.springframework.core.env.Environment env) {
        this.spaAppointmentRepository = spaAppointmentRepository;
        this.spaAppointmentTreatmentRepository = spaAppointmentTreatmentRepository;
        this.bookingRepository = bookingRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableMapper = tableMapper;
        this.auditLogService = auditLogService;
        this.openingTime = LocalTime.parse(env.getProperty("app.spa.opening-time", "09:00"));
        this.closingTime = LocalTime.parse(env.getProperty("app.spa.closing-time", "20:00"));
        this.slotMinutes = Integer.parseInt(env.getProperty("app.spa.slot-minutes", "30"));
    }

    /**
     * Batched, not one query per appointment - see CLAUDE.md's Spa billing section for the cost
     * accounting this method exists to keep small: everything below is at most a handful of
     * queries total for the whole day, never one per appointment, and the order-bearing queries
     * (orders/order items) only run for appointments that are actually COMPLETED and linked -
     * every other appointment costs nothing beyond its own row and treatments.
     */
    @Transactional(readOnly = true)
    public SpaSchedule getSchedule(LocalDate date) {
        List<Table> tables = tableRepository.findByZone(Zone.SPA).stream()
                .filter(TableEntity::isActive)
                .map(tableMapper::toDto)
                .toList();

        List<SpaAppointmentEntity> appointmentEntities = spaAppointmentRepository.findByDate(date);
        List<String> appointmentIds = appointmentEntities.stream().map(SpaAppointmentEntity::getId).toList();

        List<SpaAppointmentTreatmentEntity> allTreatmentRows = spaAppointmentTreatmentRepository.findBySpaAppointmentIdIn(appointmentIds);
        Map<String, List<SpaAppointmentTreatmentEntity>> treatmentsByAppointmentId =
                allTreatmentRows.stream().collect(Collectors.groupingBy(SpaAppointmentTreatmentEntity::getSpaAppointmentId));

        Set<String> menuItemIds = allTreatmentRows.stream().map(SpaAppointmentTreatmentEntity::getTreatmentMenuItemId).collect(Collectors.toSet());
        Map<String, MenuItemEntity> menuItemsById =
                menuItemRepository.findAllById(menuItemIds).stream().collect(Collectors.toMap(MenuItemEntity::getId, mi -> mi));

        // Only a COMPLETED appointment with a linked order can have anything to warn about - see
        // computeMissingTreatmentNames. Every other appointment skips the order/order-item reads
        // entirely, so a day with few completed-and-billed appointments stays cheap regardless of
        // how many BOOKED ones it also holds.
        Set<String> billableOrderIds = appointmentEntities.stream()
                .filter(a -> a.getStatus() == SpaAppointmentStatus.COMPLETED && a.getOrderId() != null)
                .map(SpaAppointmentEntity::getOrderId)
                .collect(Collectors.toSet());
        Map<String, OrderEntity> ordersById = orderRepository.findAllById(billableOrderIds).stream().collect(Collectors.toMap(OrderEntity::getId, o -> o));
        Map<String, List<OrderItemEntity>> orderItemsByOrderId =
                orderItemRepository.findByOrderIdIn(billableOrderIds).stream().collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        Map<String, BookingEntity> bookingsById = bookingRepository
                .findAllById(appointmentEntities.stream().map(SpaAppointmentEntity::getBookingId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(BookingEntity::getId, b -> b));
        Map<String, TableEntity> tablesById = tableRepository
                .findAllById(appointmentEntities.stream().map(SpaAppointmentEntity::getTableId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(TableEntity::getId, t -> t));
        Map<String, UserEntity> therapistsById = userRepository
                .findAllById(appointmentEntities.stream().map(SpaAppointmentEntity::getTherapistUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<SpaAppointment> appointments = appointmentEntities.stream()
                .map(e -> {
                    List<SpaAppointmentTreatmentEntity> treatmentRows = treatmentsByAppointmentId.getOrDefault(e.getId(), List.of());
                    OrderEntity order = e.getOrderId() != null ? ordersById.get(e.getOrderId()) : null;
                    boolean orderBills = order != null && order.getStatus() != OrderStatus.CANCELLED;
                    List<OrderItemEntity> orderItems = orderBills ? orderItemsByOrderId.getOrDefault(e.getOrderId(), List.of()) : List.of();
                    return buildDto(
                            e,
                            bookingsById.get(e.getBookingId()),
                            tablesById.get(e.getTableId()),
                            therapistsById.get(e.getTherapistUserId()),
                            treatmentRows,
                            menuItemsById,
                            orderBills,
                            orderItems);
                })
                .toList();

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
        MenuItemEntity treatment = validateTreatment(input.getTreatmentMenuItemId());

        LocalDate date = LocalDate.parse(input.getDate());
        LocalTime startTime = LocalTime.parse(input.getStartTime());
        int durationMinutes = treatment.getDurationMinutes();
        validateWithinOpeningHours(startTime, durationMinutes);

        SpaAppointmentEntity entity = new SpaAppointmentEntity();
        entity.setBookingId(booking.getId());
        entity.setTableId(table.getId());
        entity.setTherapistUserId(therapist.getId());
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

        insertTreatmentRow(saved.getId(), treatment);

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

    /**
     * Only legal while {@code status} is {@code BOOKED} - a {@code COMPLETED} appointment is a
     * record of what happened, not a plan still being negotiated, so growing it isn't offered
     * (see CLAUDE.md's Naming section for why this project keeps "planned" and "actually
     * happened" apart). Grows the appointment's maintained {@code durationMinutes} and lets the
     * two GiST exclusion constraints decide, same "let the database settle it" philosophy as
     * {@link #create} - no pre-check, a lost race surfaces as 409.
     */
    @Transactional
    public SpaAppointment addTreatment(String appointmentId, SpaAppointmentTreatmentCreateInput input, String actorUserId) {
        SpaAppointmentEntity entity = spaAppointmentRepository.findById(appointmentId).orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (entity.getStatus() != SpaAppointmentStatus.BOOKED) {
            throw new BadRequestException(
                    "Only a BOOKED appointment can have a treatment added - this one is " + entity.getStatus().getValue().toLowerCase());
        }
        MenuItemEntity treatment = validateTreatment(input.getTreatmentMenuItemId());

        int newDurationMinutes = entity.getDurationMinutes() + treatment.getDurationMinutes();
        validateWithinOpeningHours(entity.getStartTime(), newDurationMinutes);
        entity.setDurationMinutes(newDurationMinutes);

        SpaAppointmentEntity saved;
        try {
            saved = spaAppointmentRepository.saveAndFlush(entity);
        } catch (DataAccessException e) {
            throw translateOverlap(e);
        }

        insertTreatmentRow(saved.getId(), treatment);

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_TREATMENT_ADDED,
                AuditEntityType.SPA_APPOINTMENT,
                saved.getId(),
                "Added " + treatment.getName() + " to spa appointment " + saved.getId());

        return toDto(saved);
    }

    /**
     * Legal while {@code status} is {@code BOOKED} or {@code COMPLETED} - removing, unlike
     * adding, can only shrink the appointment's maintained {@code durationMinutes}, which can
     * never create a new overlap, so this never risks a 409 (no pre-check needed, and none of
     * the "let the database settle it" translation either) and stays allowed on a
     * {@code COMPLETED} row to correct an over-count.
     */
    @Transactional
    public SpaAppointment removeTreatment(String appointmentId, String treatmentId, String actorUserId) {
        SpaAppointmentEntity entity = spaAppointmentRepository.findById(appointmentId).orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (entity.getStatus() != SpaAppointmentStatus.BOOKED && entity.getStatus() != SpaAppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot edit treatments on a " + entity.getStatus().getValue().toLowerCase() + " appointment");
        }

        List<SpaAppointmentTreatmentEntity> treatments = spaAppointmentTreatmentRepository.findBySpaAppointmentId(appointmentId);
        SpaAppointmentTreatmentEntity toRemove = treatments.stream()
                .filter(t -> t.getId().equals(treatmentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Treatment not found on this appointment"));
        if (treatments.size() <= 1) {
            throw new BadRequestException("An appointment must have at least one treatment - cancel it instead of removing its last treatment");
        }

        spaAppointmentTreatmentRepository.delete(toRemove);
        entity.setDurationMinutes(entity.getDurationMinutes() - toRemove.getDurationMinutes());
        SpaAppointmentEntity saved = spaAppointmentRepository.saveAndFlush(entity);

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_TREATMENT_REMOVED,
                AuditEntityType.SPA_APPOINTMENT,
                saved.getId(),
                "Removed a treatment from spa appointment " + saved.getId());

        return toDto(saved);
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
     * {@code durationMinutes} is untouched - moving an appointment doesn't change its treatments,
     * only where/when they happen. Same "let the database settle it" philosophy as
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

    private MenuItemEntity validateTreatment(String treatmentMenuItemId) {
        MenuItemEntity treatment =
                menuItemRepository.findById(treatmentMenuItemId).orElseThrow(() -> new NotFoundException("Treatment not found"));
        if (treatment.getDepartment() != MenuDepartment.SPA || treatment.getDurationMinutes() == null) {
            throw new BadRequestException("treatmentMenuItemId must reference a SPA-department item with a duration set");
        }
        return treatment;
    }

    private SpaAppointmentTreatmentEntity insertTreatmentRow(String appointmentId, MenuItemEntity treatment) {
        SpaAppointmentTreatmentEntity row = new SpaAppointmentTreatmentEntity();
        row.setSpaAppointmentId(appointmentId);
        row.setTreatmentMenuItemId(treatment.getId());
        row.setDurationMinutes(treatment.getDurationMinutes());
        return spaAppointmentTreatmentRepository.saveAndFlush(row);
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

        List<SpaAppointmentTreatmentEntity> treatmentRows = spaAppointmentTreatmentRepository.findBySpaAppointmentId(e.getId());
        Map<String, MenuItemEntity> menuItemsById = menuItemRepository
                .findAllById(treatmentRows.stream().map(SpaAppointmentTreatmentEntity::getTreatmentMenuItemId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, mi -> mi));

        boolean orderBills = false;
        List<OrderItemEntity> orderItems = List.of();
        if (e.getStatus() == SpaAppointmentStatus.COMPLETED && e.getOrderId() != null) {
            OrderEntity order = orderRepository.findById(e.getOrderId()).orElse(null);
            if (order != null && order.getStatus() != OrderStatus.CANCELLED) {
                orderBills = true;
                orderItems = orderItemRepository.findByOrderId(e.getOrderId());
            }
        }

        return buildDto(e, booking, table, therapist, treatmentRows, menuItemsById, orderBills, orderItems);
    }

    private SpaAppointment buildDto(
            SpaAppointmentEntity e,
            BookingEntity booking,
            TableEntity table,
            UserEntity therapist,
            List<SpaAppointmentTreatmentEntity> treatmentRows,
            Map<String, MenuItemEntity> menuItemsById,
            boolean orderBills,
            List<OrderItemEntity> orderItems) {
        List<SpaAppointmentTreatment> treatments = treatmentRows.stream()
                .map(t -> {
                    MenuItemEntity menuItem = menuItemsById.get(t.getTreatmentMenuItemId());
                    return new SpaAppointmentTreatment(t.getId(), t.getTreatmentMenuItemId(), menuItem.getName(), t.getDurationMinutes(), menuItem.getPrice().toString());
                })
                .toList();

        List<String> missingTreatmentNames = computeMissingTreatmentNames(e.getStatus(), treatmentRows, menuItemsById, orderBills, orderItems);

        return new SpaAppointment(
                e.getId(),
                e.getBookingId(),
                booking.getGuestName(),
                e.getTableId(),
                table.getLabel(),
                e.getTherapistUserId(),
                therapist.getEmail(),
                treatments,
                e.getDate().toString(),
                e.getStartTime().format(TIME_FORMAT),
                e.getDurationMinutes(),
                e.getStatus(),
                e.getOrderId(),
                missingTreatmentNames,
                e.getCreatedByUserId(),
                e.getCancelledByUserId(),
                e.getCancelReason(),
                TimestampFormat.toUtc(e.getCreatedAt()),
                TimestampFormat.toUtc(e.getUpdatedAt()));
    }

    /**
     * The completeness warning: which treatment names this appointment holds that the linked
     * order doesn't (yet) carry enough of - a multiset comparison, not a per-line trace, so
     * adding no new column to OrderItem. Only meaningful once {@code status} is
     * {@code COMPLETED} - every other status returns no warning regardless of {@code orderId},
     * see {@code SpaAppointment.missingTreatmentNames}'s own openapi.yaml description.
     * {@code orderBills} is already false (by both callers, see {@code toDto}/{@code getSchedule})
     * whenever the linked order doesn't exist or is CANCELLED - a cancelled order bills nothing,
     * so it counts here as carrying none of this appointment's treatments, same as no order at
     * all.
     */
    private static List<String> computeMissingTreatmentNames(
            SpaAppointmentStatus status,
            List<SpaAppointmentTreatmentEntity> treatmentRows,
            Map<String, MenuItemEntity> menuItemsById,
            boolean orderBills,
            List<OrderItemEntity> orderItems) {
        if (status != SpaAppointmentStatus.COMPLETED) {
            return List.of();
        }

        Map<String, Integer> billedQuantityByMenuItemId = new HashMap<>();
        if (orderBills) {
            for (OrderItemEntity item : orderItems) {
                billedQuantityByMenuItemId.merge(item.getMenuItemId(), item.getQuantity(), Integer::sum);
            }
        }

        Map<String, Long> bookedCountByMenuItemId =
                treatmentRows.stream().collect(Collectors.groupingBy(SpaAppointmentTreatmentEntity::getTreatmentMenuItemId, Collectors.counting()));

        List<String> missing = new ArrayList<>();
        Set<String> reported = new HashSet<>();
        for (SpaAppointmentTreatmentEntity row : treatmentRows) {
            String menuItemId = row.getTreatmentMenuItemId();
            if (!reported.add(menuItemId)) {
                continue;
            }
            int billed = billedQuantityByMenuItemId.getOrDefault(menuItemId, 0);
            long booked = bookedCountByMenuItemId.get(menuItemId);
            if (billed < booked) {
                missing.add(menuItemsById.get(menuItemId).getName());
            }
        }
        return missing;
    }
}
