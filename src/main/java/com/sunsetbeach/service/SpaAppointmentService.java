package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.SpaAppointmentOrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.SpaAppointmentTreatmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.SqlStates;
import com.sunsetbeach.mapper.TableMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
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
import com.sunsetbeach.model.SwapSpaAppointmentTableInput;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.SpaAppointmentOrderRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.SpaAppointmentTreatmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String EXCLUSION_VIOLATION_SQLSTATE = "23P01";
    private static final String DEADLOCK_DETECTED_SQLSTATE = "40P01";

    private final SpaAppointmentRepository spaAppointmentRepository;
    private final SpaAppointmentTreatmentRepository spaAppointmentTreatmentRepository;
    private final BookingRepository bookingRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SpaAppointmentOrderRepository spaAppointmentOrderRepository;
    private final TableMapper tableMapper;
    private final AuditLogService auditLogService;
    private final OverstayRule overstayRule;
    private final java.time.Clock clock;
    private final BookingSegmentRepository bookingSegmentRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final OrderService orderService;
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
            SpaAppointmentOrderRepository spaAppointmentOrderRepository,
            TableMapper tableMapper,
            AuditLogService auditLogService,
            OverstayRule overstayRule,
            BookingSegmentRepository bookingSegmentRepository,
            RoomUnitRepository roomUnitRepository,
            OrderService orderService,
            java.time.Clock clock,
            org.springframework.core.env.Environment env) {
        this.clock = clock;
        this.overstayRule = overstayRule;
        this.bookingSegmentRepository = bookingSegmentRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.orderService = orderService;
        this.spaAppointmentRepository = spaAppointmentRepository;
        this.spaAppointmentTreatmentRepository = spaAppointmentTreatmentRepository;
        this.bookingRepository = bookingRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.spaAppointmentOrderRepository = spaAppointmentOrderRepository;
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
        List<String> completedIds = appointmentEntities.stream()
                .filter(a -> a.getStatus() == SpaAppointmentStatus.COMPLETED)
                .map(SpaAppointmentEntity::getId)
                .toList();
        // An appointment can be billed by several orders (its own plus top-ups, V144): the warning
        // looks at all of them together.
        Map<String, List<String>> extraOrderIdsByAppointmentId = spaAppointmentOrderRepository.findBySpaAppointmentIdIn(completedIds).stream()
                .collect(Collectors.groupingBy(SpaAppointmentOrderEntity::getSpaAppointmentId,
                        Collectors.mapping(SpaAppointmentOrderEntity::getOrderId, Collectors.toList())));
        Map<String, List<String>> orderIdsByAppointmentId = new HashMap<>();
        Set<String> billableOrderIds = new HashSet<>();
        for (SpaAppointmentEntity a : appointmentEntities) {
            if (a.getStatus() != SpaAppointmentStatus.COMPLETED) {
                continue;
            }
            List<String> ids = new ArrayList<>();
            if (a.getOrderId() != null) {
                ids.add(a.getOrderId());
            }
            ids.addAll(extraOrderIdsByAppointmentId.getOrDefault(a.getId(), List.of()));
            orderIdsByAppointmentId.put(a.getId(), ids);
            billableOrderIds.addAll(ids);
        }
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

        Map<String, String> roomUnitLabelsByBookingId = roomUnitLabelsOn(date, bookingsById.keySet());

        List<SpaAppointment> appointments = appointmentEntities.stream()
                .map(e -> {
                    List<SpaAppointmentTreatmentEntity> treatmentRows = treatmentsByAppointmentId.getOrDefault(e.getId(), List.of());
                    List<OrderEntity> billing = orderIdsByAppointmentId.getOrDefault(e.getId(), List.of()).stream()
                            .map(ordersById::get)
                            .filter(o -> o != null && o.getStatus() != OrderStatus.CANCELLED)
                            .toList();
                    boolean orderBills = !billing.isEmpty();
                    List<OrderItemEntity> orderItems = billing.stream()
                            .flatMap(o -> orderItemsByOrderId.getOrDefault(o.getId(), List.of()).stream())
                            .toList();
                    SpaAppointment dto = buildDto(
                            e,
                            bookingsById.get(e.getBookingId()),
                            tablesById.get(e.getTableId()),
                            therapistsById.get(e.getTherapistUserId()),
                            treatmentRows,
                            menuItemsById,
                            orderBills,
                            orderItems);
                    dto.setRoomUnitLabel(roomUnitLabelsByBookingId.get(e.getBookingId()));
                    return dto;
                })
                .toList();

        return new SpaSchedule(date.toString(), openingTime.format(TIME_FORMAT), closingTime.format(TIME_FORMAT), slotMinutes, tables, appointments);
    }

    @Transactional(readOnly = true)
    public List<SpaTherapist> listTherapists() {
        return userRepository.findAll().stream()
                .filter(UserEntity::isActive)
                .filter(u -> Arrays.asList(u.getJobFunctions()).contains(JobFunction.THERAPIST.getValue()))
                .map(u -> new SpaTherapist(u.getId(), u.getName()).email(u.getEmail()))
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
        requireTherapist(therapist);
        requireSpaTable(table);
        requireServable(booking, date);
        requireNotInThePast(date, startTime);
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
                        + table.getLabel() + ", " + therapist.getName() + ")");

        // No out-of-stay warning any more: requireServable refuses a date outside the stay outright.
        String warning = null;

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
        if (isOrderPaid(entity)) {
            throw new ConflictException("This appointment's order is already paid, so a treatment can't be removed from it. Refund through the order instead.");
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
        BookingEntity booking = bookingRepository.findById(entity.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));
        requireTherapist(therapist);
        requireSpaTable(table);
        requireServable(booking, date);
        requireNotInThePast(date, startTime);
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
                "Rescheduled to " + date + " " + startTime.format(TIME_FORMAT) + " (" + table.getLabel() + ", " + therapist.getName() + ")");

        return toDto(saved);
    }

    /**
     * Swaps two appointments' tables, atomically. Table only - {@code date}, {@code startTime},
     * {@code therapistUserId}, and the frozen {@code durationMinutes} are untouched on both
     * sides, which is what makes this well-defined no matter how long either treatment runs: a
     * 30-minute treatment and a 90-minute one trade tables exactly the same way two equal-length
     * ones would, because nothing about "how long" or "when" ever moves. A full slot exchange
     * (time included) would only stay coherent between equal-length treatments and isn't offered
     * here - see {@code SwapSpaAppointmentTableInput}'s own description.
     *
     * <p>Both appointments must be {@code BOOKED} - the same lifecycle boundary
     * {@link #updateSchedule} already enforces.
     *
     * <p><b>Mechanism.</b> A genuine swap can only be expressed as two ordinary {@code UPDATE}s
     * (each appointment keeps its own row; only {@code tableId} moves), and under the default
     * constraint timing that's unworkable: {@code spa_appointment_no_table_overlap} was
     * {@code NOT DEFERRABLE}, checked synchronously as each row is written, so the first
     * {@code UPDATE} always finds the other appointment still sitting on the table it's moving
     * into. A single statement moving both rows at once was tried and fails the same way - see
     * {@link SpaAppointmentTableSwapConstraintTimingTests}, kept specifically as the record of
     * why that doesn't work, and CLAUDE.md's Concurrency section for the same reasoning written
     * down for whoever reaches for a swap next. V56's migration made both exclusion constraints
     * {@code DEFERRABLE INITIALLY IMMEDIATE}, so this method can defer them for just this
     * transaction ({@link SpaAppointmentRepository#deferOverlapConstraints}), apply both ordinary
     * updates, then force the checks to run immediately, right here
     * ({@link SpaAppointmentRepository#restoreImmediateOverlapConstraints}) - proven correct
     * (both the success and the still-genuinely-conflicting case) in
     * {@link SpaAppointmentTableSwapDeferredConstraintTests} before this method was written to
     * depend on it.
     *
     * <p>{@code saveAndFlush}, not {@code save}, for both updates: forcing each one to the
     * database immediately, in this exact order, is what makes "defer, move both, force the
     * check" a real sequence rather than something Hibernate's own write-behind batching could
     * reorder or coalesce - restoreImmediateOverlapConstraints only means what it's supposed to
     * mean if both UPDATEs have actually reached Postgres by the time it runs.
     *
     * <p><b>The audit entry only follows a confirmed swap.</b> {@code AuditLogService.record} runs
     * {@code REQUIRES_NEW} and commits independently and immediately - had either record() call
     * happened before {@code restoreImmediateOverlapConstraints()} returned successfully, a swap
     * that fails at that check would still leave a permanent audit entry describing a swap that
     * never happened (this is exactly the hazard that ruled out deferring the check all the way
     * to this transaction's own COMMIT: nothing between here and commit would still be able to
     * stop a REQUIRES_NEW write that already happened). Both records only run after both
     * {@code saveAndFlush} calls and the forced check have all already succeeded.
     *
     * <p><b>Concurrency.</b> No SERIALIZABLE, no pre-check query - same "let the database settle
     * it" philosophy as every other write in this service. Two swaps racing to touch the *same*
     * appointment serialize on Postgres's own row-level lock: whichever transaction's
     * {@code UPDATE} reaches that row first holds it until it commits or rolls back, and the
     * second one simply waits, then proceeds against the already-updated state - ordinary MVCC,
     * nothing extra needed *provided* both transactions try to lock the two rows in the same
     * order. They don't, by default: this method updates whichever appointment the caller named
     * first, then the other - so two swaps naming the same pair of appointments in opposite order
     * (two receptionists, one clicking swap from A's card and the other from B's, at the same two
     * appointments) would each lock one row and then wait for the other, a deadlock reachable on
     * an ordinary Tuesday rather than adversarial bad luck. The two {@code saveAndFlush} calls
     * below are therefore ordered by the appointments' own ids, not by which one the caller called
     * {@code id} versus {@code otherAppointmentId} - the same two rows get locked in the same
     * order no matter which side either caller's request names first, which removes this cycle
     * outright rather than translating it after the fact.
     *
     * <p>A third, unrelated write (an ordinary {@code create}/{@code updateSchedule}) racing for
     * one of the two tables this swap is moving appointments onto is handled by PostgreSQL's own
     * exclusion-constraint machinery: an immediate-checked writer that finds a possibly-
     * conflicting, not-yet-committed row from this (deferred) transaction waits for it to resolve
     * rather than erroring out early, then re-checks against whatever actually got committed. This
     * *can* still deadlock, unlike the same-pair-of-swaps case above - not because either side
     * locked rows out of order, but because each side's own exclusion-constraint check can end up
     * needing a lock the other side's uncommitted row is holding, in both directions at once. That
     * shape isn't something a lock order fixes (the two sides aren't contending for the same two
     * rows in the first place; one is an `UPDATE`'s check, the other an `INSERT`'s), so instead of
     * preventing it, {@link #translateOverlap} recognises it by its own SQLSTATE and returns the
     * same "try again" conflict {@code create}/{@code updateSchedule} would give for a genuine
     * overlap - the transaction Postgres kills to break the deadlock is retried by a person, same
     * as any other race in this system, not silently retried by this method itself.
     */
    @Transactional
    public SpaAppointment swapTables(String id, SwapSpaAppointmentTableInput input, String actorUserId) {
        String otherId = input.getOtherAppointmentId();
        if (otherId.equals(id)) {
            throw new BadRequestException("Can't swap an appointment with itself");
        }
        SpaAppointmentEntity entity = spaAppointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found"));
        SpaAppointmentEntity other = spaAppointmentRepository.findById(otherId).orElseThrow(() -> new NotFoundException("Other appointment not found"));
        if (entity.getStatus() != SpaAppointmentStatus.BOOKED || other.getStatus() != SpaAppointmentStatus.BOOKED) {
            throw new BadRequestException("Both appointments must be BOOKED to swap tables");
        }

        String entityOldTableId = entity.getTableId();
        String otherOldTableId = other.getTableId();
        BookingEntity entityBooking = bookingRepository.findById(entity.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingEntity otherBooking = bookingRepository.findById(other.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));
        TableEntity entityOldTable = tableRepository.findById(entityOldTableId).orElseThrow(() -> new NotFoundException("Table not found"));
        TableEntity otherOldTable = tableRepository.findById(otherOldTableId).orElseThrow(() -> new NotFoundException("Table not found"));

        // Lock the two rows in a deterministic (lowest id first) order, not in whichever order
        // the caller happened to name entity/other - see this method's own Concurrency section.
        SpaAppointmentEntity first = entity.getId().compareTo(other.getId()) <= 0 ? entity : other;
        SpaAppointmentEntity second = first == entity ? other : entity;
        try {
            // deferOverlapConstraints() must run before either setTableId() call below, never
            // after: it's a native query, and Hibernate's default flush mode flushes any
            // already-dirty entity before running one (a native statement's own dependencies
            // aren't visible to it, so it flushes conservatively rather than risk running against
            // stale data). Move the two setTableId() calls above this line and both UPDATEs go
            // out while the constraints are still IMMEDIATE - deferral never actually takes
            // effect, and every swap starts failing with a genuine, self-inflicted overlap
            // violation against its own two rows. This looks like a harmless statement reorder;
            // it isn't - it silently broke this method while the lock-order fix above was being
            // built, caught only because the race test's own outcome changed, not because
            // anything failed to compile or an existing assertion tripped.
            spaAppointmentRepository.deferOverlapConstraints();
            entity.setTableId(otherOldTableId);
            other.setTableId(entityOldTableId);
            spaAppointmentRepository.saveAndFlush(first);
            spaAppointmentRepository.saveAndFlush(second);
            spaAppointmentRepository.restoreImmediateOverlapConstraints();
        } catch (DataAccessException e) {
            throw translateOverlap(e);
        }

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_TABLES_SWAPPED,
                AuditEntityType.SPA_APPOINTMENT,
                entity.getId(),
                "Swapped tables with " + otherBooking.getGuestName() + "'s appointment: now at " + otherOldTable.getLabel() + " (was "
                        + entityOldTable.getLabel() + ")");
        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_TABLES_SWAPPED,
                AuditEntityType.SPA_APPOINTMENT,
                other.getId(),
                "Swapped tables with " + entityBooking.getGuestName() + "'s appointment: now at " + entityOldTable.getLabel() + " (was "
                        + otherOldTable.getLabel() + ")");

        return toDto(entity);
    }

    /**
     * A therapist is an active account holding the {@code THERAPIST} job function - the same test
     * {@link #listTherapists} uses to offer people in the picker, enforced here on the server so a
     * stale screen, an old link or a direct API call can't book someone who isn't one.
     */
    private static void requireTherapist(UserEntity therapist) {
        if (!therapist.isActive()) {
            throw new BadRequestException(therapist.getName() + "'s account is deactivated, so they can't be booked for a treatment.");
        }
        if (!Arrays.asList(therapist.getJobFunctions()).contains(JobFunction.THERAPIST.getValue())) {
            throw new BadRequestException(therapist.getName() + " is not a therapist (the Therapist job function is not set on their account).");
        }
    }

    /** A treatment happens in the spa zone; a restaurant table has no business on the spa grid. */
    private static void requireSpaTable(TableEntity table) {
        if (table.getZone() != Zone.SPA) {
            throw new BadRequestException("Table " + table.getLabel() + " is not a spa table - treatments can only be booked on tables in the SPA zone.");
        }
    }

    /**
     * Whom the spa can serve on {@code date}. A guest who has left, a cancelled booking and a
     * no-show can't be booked (409 - the booking is in the wrong state). Everyone else - in the
     * house or not yet arrived, whatever the booking's status ({@code NEW}/{@code CONFIRMED}/
     * {@code PAID}) - can be booked only on a day of the stay they actually hold, both ends
     * included, since the guest is still here on the departure day (400 - the date is wrong). A
     * checked-in guest is held to it too: a treatment after their checkout date would outlive the
     * stay (and a shortened stay cancels the ones that fall outside it), so the stay is extended
     * first.
     */
    private static void requireServable(BookingEntity booking, LocalDate date) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException(booking.getGuestName() + "'s booking is cancelled, so no treatment can be booked for it.");
        }
        switch (booking.getOccupancyStatus()) {
            case CHECKED_OUT -> throw new ConflictException(booking.getGuestName() + " has already checked out, so no treatment can be booked for this booking.");
            case NO_SHOW -> throw new ConflictException(booking.getGuestName() + " was marked as a no-show, so no treatment can be booked for this booking.");
            case CHECKED_IN, EXPECTED -> {
                if (date.isBefore(booking.getCheckIn()) || date.isAfter(booking.getCheckOut())) {
                    throw new BadRequestException(booking.getGuestName() + "'s stay is " + booking.getCheckIn() + " to " + booking.getCheckOut()
                            + ", which doesn't include " + date + " - pick a date within the stay.");
                }
            }
        }
    }

    /**
     * A treatment can't start in the past - but the slot that is under way can still be booked
     * (it is 10:20 and the 10:00 slot is open: a walk-in is fine). The slots are the grid from
     * opening time, {@code slotMinutes} apart, in the hotel's time.
     */
    private void requireNotInThePast(LocalDate date, LocalTime startTime) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        if (date.isBefore(today)) {
            throw new BadRequestException("The date " + date + " has already passed - pick today or a later day.");
        }
        if (date.isAfter(today)) {
            return;
        }
        LocalTime nowTime = now.toLocalTime();
        LocalTime currentSlotStart = nowTime.isBefore(openingTime)
                ? openingTime
                : openingTime.plusMinutes((Duration.between(openingTime, nowTime).toMinutes() / slotMinutes) * slotMinutes);
        if (startTime.isBefore(currentSlotStart)) {
            throw new BadRequestException("The time " + startTime.format(TIME_FORMAT) + " has already passed - the earliest start now is "
                    + currentSlotStart.format(TIME_FORMAT) + ".");
        }
    }

    private boolean isOrderPaid(SpaAppointmentEntity appointment) {
        return linkedOrders(appointment).stream().anyMatch(o -> o.getStatus() == OrderStatus.PAID);
    }

    /**
     * Every order that bills this appointment: its own ({@code orderId}) first, then the top-up
     * orders (V144) oldest first.
     */
    private List<OrderEntity> linkedOrders(SpaAppointmentEntity appointment) {
        List<String> ids = new ArrayList<>();
        if (appointment.getOrderId() != null) {
            ids.add(appointment.getOrderId());
        }
        spaAppointmentOrderRepository.findBySpaAppointmentId(appointment.getId()).stream()
                .sorted(Comparator.comparing(SpaAppointmentOrderEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(link -> ids.add(link.getOrderId()));
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, OrderEntity> byId = orderRepository.findAllById(ids).stream().collect(Collectors.toMap(OrderEntity::getId, o -> o));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    /**
     * The partial-payment top-up: a {@code COMPLETED} appointment whose orders don't carry every
     * treatment gets exactly the missing quantity of each one billed, in one action. What is
     * "carried" is counted over all of the appointment's orders (its own and earlier top-ups,
     * cancelled ones excluded). If one of them can still take lines ({@code OPEN}/{@code SENT}) the
     * missing treatments go onto it; if every order is already {@code PAID} (or cancelled) a new
     * order for just the missing treatments is opened on the appointment's table and linked to the
     * same appointment (V144), so a guest who paid for one treatment and then took a second pays
     * for the second on its own bill. Either way the lines go through {@link OrderService#addItems},
     * so the price is the menu's live one at this moment (the one pricing rule for POS lines, see
     * CLAUDE.md's Spa billing section); nothing is charged here - the order is closed as usual.
     */
    @Transactional
    public SpaAppointment billMissingTreatments(String appointmentId, String actorUserId) {
        SpaAppointmentEntity entity = spaAppointmentRepository.findById(appointmentId).orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (entity.getStatus() != SpaAppointmentStatus.COMPLETED) {
            throw new ConflictException("Only a completed appointment's treatments can be added to its bill - this one is " + entity.getStatus().getValue().toLowerCase() + ".");
        }
        List<OrderEntity> orders = linkedOrders(entity);
        if (orders.isEmpty()) {
            throw new ConflictException("This appointment has no order yet - open the spa billing to create its bill.");
        }

        Map<String, Integer> billed = new HashMap<>();
        for (OrderEntity linked : orders) {
            if (linked.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            for (OrderItemEntity item : orderItemRepository.findByOrderId(linked.getId())) {
                billed.merge(item.getMenuItemId(), item.getQuantity(), Integer::sum);
            }
        }
        Map<String, Long> booked = spaAppointmentTreatmentRepository.findBySpaAppointmentId(appointmentId).stream()
                .collect(Collectors.groupingBy(SpaAppointmentTreatmentEntity::getTreatmentMenuItemId, java.util.LinkedHashMap::new, Collectors.counting()));
        List<OrderItemInput> missing = new ArrayList<>();
        booked.forEach((menuItemId, count) -> {
            long shortBy = count - billed.getOrDefault(menuItemId, 0);
            if (shortBy > 0) {
                missing.add(new OrderItemInput(menuItemId, Math.toIntExact(shortBy)));
            }
        });
        if (missing.isEmpty()) {
            throw new ConflictException("Everything this appointment booked is already on its orders.");
        }

        OrderEntity target = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN || o.getStatus() == OrderStatus.SENT)
                .findFirst()
                .orElse(null);
        boolean newOrder = target == null;
        String targetId;
        if (newOrder) {
            // Created empty (a spa-zone table is exempt from "an order starts with its first line"),
            // linked, and only then given its lines - the link must exist before addItems so the
            // spa auto-link can't attach this order to some other appointment on the same table.
            Order created = orderService.create(new OrderCreateInput().tableId(entity.getTableId()), actorUserId);
            spaAppointmentOrderRepository.saveAndFlush(new SpaAppointmentOrderEntity(created.getId(), entity.getId()));
            targetId = created.getId();
        } else {
            targetId = target.getId();
        }

        Order result = orderService.addItems(targetId, missing);

        auditLogService.record(
                AuditAction.SPA_APPOINTMENT_TREATMENT_ADDED,
                AuditEntityType.SPA_APPOINTMENT,
                entity.getId(),
                "Added the missing treatments (" + missing.stream().map(i -> i.getQuantity() + "x " + treatmentName(i.getMenuItemId())).collect(Collectors.joining(", "))
                        + ") to " + (newOrder ? "a new order #" : "order #") + result.getNumber()
                        + (newOrder ? " (the earlier order can't take more lines)" : ""));
        return toDto(entity);
    }

    private String treatmentName(String menuItemId) {
        return menuItemRepository.findById(menuItemId).map(MenuItemEntity::getName).orElse(menuItemId);
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
     * Recognises the failure by its own SQLSTATE (see {@link SqlStates} and CLAUDE.md's
     * Concurrency section), never by matching prose in the error message - message wording isn't
     * an API, and a Postgres deadlock's own message doesn't name a constraint at all (it names the
     * relation it was checking one against), which is exactly why the string-matching this method
     * used to do was blind to it: a genuine two-writer deadlock reaching this method (racing an
     * ordinary {@code create}/{@code updateSchedule} against this service's own deferred-then-
     * forced-immediate check in {@link #swapTables}, or two swaps against each other before the
     * fix described there) surfaced as a raw, untranslated {@link DataAccessException} instead of
     * the same "try again" conflict every other race in this system already gives.
     *
     * <p>{@code 23P01} (exclusion_violation) is a genuine double-booking: the constraint name -
     * read from the driver's own {@code SQLException}, not re-derived from the outer message -
     * says which axis lost the race (see V41__spa_appointment.sql). {@code 40P01} (deadlock
     * detected) is not a booking conflict at all, just two transactions that got in each other's
     * way; nothing here is actually wrong with either write, so it gets the same generic
     * "someone else, try again" shape {@link BookingService#isSerializationFailure} already gives
     * for {@code 40001}, deliberately not one of the two overlap-specific sentences (this method
     * doesn't know, and shouldn't guess, which side of a deadlock supposedly "lost"). Rethrows
     * unrecognized failures untranslated rather than masking them as a generic conflict -
     * {@link com.sunsetbeach.error.GlobalExceptionHandler}'s own database-failure backstop is
     * where those end up, not a manufactured overlap message here.
     */
    static ConflictException translateOverlap(DataAccessException e) {
        if (SqlStates.is(e, DEADLOCK_DETECTED_SQLSTATE)) {
            return new ConflictException("Someone else was changing one of these appointments at the same time — please try again.");
        }
        Optional<SQLException> exclusionViolation = SqlStates.find(e, EXCLUSION_VIOLATION_SQLSTATE);
        if (exclusionViolation.isPresent()) {
            String message = exclusionViolation.get().getMessage();
            if (message != null && message.contains("spa_appointment_no_table_overlap")) {
                return new ConflictException("This table already has an appointment overlapping this time.");
            }
            if (message != null && message.contains("spa_appointment_no_therapist_overlap")) {
                return new ConflictException("This therapist already has an appointment overlapping this time.");
            }
        }
        throw e;
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
        if (e.getStatus() == SpaAppointmentStatus.COMPLETED) {
            List<OrderEntity> billing = linkedOrders(e).stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
            orderBills = !billing.isEmpty();
            orderItems = billing.isEmpty() ? List.of() : orderItemRepository.findByOrderIdIn(billing.stream().map(OrderEntity::getId).toList());
        }

        SpaAppointment dto = buildDto(e, booking, table, therapist, treatmentRows, menuItemsById, orderBills, orderItems);
        dto.setRoomUnitLabel(roomUnitLabelsOn(e.getDate(), List.of(e.getBookingId())).get(e.getBookingId()));
        return dto;
    }

    /**
     * {@code SpaAppointment.roomUnitLabel}: the room each booking occupies on {@code date}, read
     * from its segments so a relocated stay names the room the guest is actually in that day.
     * Outside the stay, the nearest segment - the last one from the departure day on (which is
     * also the room an overdue guest still holds, see {@link OverstayRule}), the first one before
     * arrival. Two queries for any number of bookings. A booking whose segment has no unit yet is
     * simply missing from the result.
     */
    private Map<String, String> roomUnitLabelsOn(LocalDate date, Collection<String> bookingIds) {
        if (bookingIds.isEmpty()) return Map.of();
        Map<String, List<BookingSegmentEntity>> segmentsByBookingId = bookingSegmentRepository
                .findByBookingIdIn(List.copyOf(bookingIds))
                .stream()
                .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));

        Map<String, String> unitIdByBookingId = new HashMap<>();
        segmentsByBookingId.forEach((bookingId, segments) -> {
            List<BookingSegmentEntity> ordered =
                    segments.stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();
            BookingSegmentEntity onDate = ordered.stream()
                    .filter(seg -> !date.isBefore(seg.getCheckIn()) && date.isBefore(seg.getCheckOut()))
                    .findFirst()
                    .orElse(date.isBefore(ordered.get(0).getCheckIn()) ? ordered.get(0) : ordered.get(ordered.size() - 1));
            if (onDate.getRoomUnitId() != null) unitIdByBookingId.put(bookingId, onDate.getRoomUnitId());
        });

        Map<String, String> labelsByUnitId = roomUnitRepository.findAllById(new HashSet<>(unitIdByBookingId.values())).stream()
                .collect(Collectors.toMap(RoomUnitEntity::getId, RoomUnitEntity::getLabel));
        Map<String, String> result = new HashMap<>();
        unitIdByBookingId.forEach((bookingId, unitId) -> {
            String label = labelsByUnitId.get(unitId);
            if (label != null) result.put(bookingId, label);
        });
        return result;
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

        SpaAppointment dto = new SpaAppointment(
                e.getId(),
                e.getBookingId(),
                booking.getGuestName(),
                e.getTableId(),
                table.getLabel(),
                e.getTherapistUserId(),
                therapist.getName(),
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
        dto.setTherapistEmail(therapist.getEmail());
        return dto;
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
