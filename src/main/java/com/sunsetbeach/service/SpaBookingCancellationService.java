package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.SpaAppointmentOrderEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.SpaAppointmentOrderRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.security.IntegrationPrincipal;
import com.sunsetbeach.security.StaffPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a cancelled booking does to the guest's spa. Called from {@link BookingService#updateStatus}
 * inside its own transaction (a cancellation that left a therapist and a table held for a guest
 * who is not coming is the failure this exists to prevent), and from the unconfirmed-booking
 * expiry sweep.
 *
 * <p>Every {@code BOOKED} appointment of the booking is cancelled, and the still-unpaid spa lines
 * on the order linked to it are taken off ({@link OrderService#removeUnpaidSpaLines}; an order left
 * empty is cancelled). <b>Anything already paid is left exactly as it is</b> - the appointment
 * stays {@code BOOKED}, its order untouched - and is reported by {@link #paidLeftBehind} instead,
 * because undoing money is a decision for a person (a refund), not a side effect of a status
 * change. A {@code COMPLETED} appointment is a treatment that was delivered: it keeps its status
 * and its order, paid or not, and is never part of that warning.
 */
@Service
public class SpaBookingCancellationService {

    private static final String CANCEL_REASON = "Booking cancelled";

    private final SpaAppointmentRepository spaAppointmentRepository;
    private final SpaAppointmentOrderRepository spaAppointmentOrderRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final OrderService orderService;
    private final AuditLogService auditLogService;

    public SpaBookingCancellationService(
            SpaAppointmentRepository spaAppointmentRepository,
            SpaAppointmentOrderRepository spaAppointmentOrderRepository,
            OrderRepository orderRepository,
            Clock clock,
            OrderService orderService,
            AuditLogService auditLogService) {
        this.spaAppointmentRepository = spaAppointmentRepository;
        this.spaAppointmentOrderRepository = spaAppointmentOrderRepository;
        this.clock = clock;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
    }

    /** The clean-up for a booking that was just cancelled; returns {@link #paidLeftBehind}'s text, or null. */
    @Transactional
    public String onBookingCancelled(BookingEntity booking) {
        for (SpaAppointmentEntity appointment : spaAppointmentRepository.findByBookingId(booking.getId())) {
            cancelBooked(appointment, "Spa appointment " + appointment.getId() + " marked cancelled - " + booking.getGuestName() + "'s booking was cancelled");
        }
        return paidLeftBehind(booking.getId());
    }

    /**
     * The stay's dates changed (a shortened stay, an early departure's new check-out, a schedule
     * edit, a SiteMinder date change): every {@code BOOKED} appointment that has not ended and now
     * lies outside {@code checkIn..checkOut} (both ends inclusive - the guest is still here on the
     * departure day) is cancelled, the same cascade as a cancelled booking, in the same
     * transaction. A paid appointment is left as it is. Returns how many were cancelled.
     */
    @Transactional
    public int onStayChanged(BookingEntity booking) {
        LocalDateTime now = LocalDateTime.now(clock);
        int cancelled = 0;
        for (SpaAppointmentEntity appointment : spaAppointmentRepository.findByBookingId(booking.getId())) {
            boolean outside = appointment.getDate().isBefore(booking.getCheckIn()) || appointment.getDate().isAfter(booking.getCheckOut());
            LocalDateTime end = LocalDateTime.of(appointment.getDate(), appointment.getStartTime()).plusMinutes(appointment.getDurationMinutes());
            if (outside && end.isAfter(now)
                    && cancelBooked(appointment, "Spa appointment " + appointment.getId() + " marked cancelled - " + booking.getGuestName() + "'s stay is now "
                            + booking.getCheckIn() + " to " + booking.getCheckOut())) {
                cancelled++;
            }
        }
        return cancelled;
    }

    /**
     * Cancels one appointment if it is {@code BOOKED} and nothing on it is paid: the unpaid spa
     * lines come off every order that bills it. Returns whether it was cancelled.
     */
    private boolean cancelBooked(SpaAppointmentEntity appointment, String auditSummary) {
        if (appointment.getStatus() != SpaAppointmentStatus.BOOKED) {
            return false;
        }
        List<OrderEntity> orders = linkedOrders(appointment);
        if (orders.stream().anyMatch(o -> o.getStatus() == OrderStatus.PAID)) {
            return false; // paid: left as it is, reported by paidLeftBehind
        }

        StringBuilder lines = new StringBuilder();
        for (OrderEntity order : orders) {
            OrderService.SpaLineCleanup cleanup = orderService.removeUnpaidSpaLines(order.getId());
            if (!cleanup.removedLines().isEmpty()) {
                lines.append("; took ").append(String.join(", ", cleanup.removedLines())).append(" off order #").append(order.getNumber())
                        .append(cleanup.orderCancelled() ? " (order cancelled, nothing else was on it)" : "");
            }
        }
        appointment.setStatus(SpaAppointmentStatus.CANCELLED);
        appointment.setCancelledByUserId(staffActorId());
        appointment.setCancelReason(CANCEL_REASON);
        spaAppointmentRepository.saveAndFlush(appointment);
        record(AuditAction.SPA_APPOINTMENT_STATUS_CHANGED, AuditEntityType.SPA_APPOINTMENT, appointment.getId(), auditSummary + lines);
        return true;
    }

    /**
     * The warning for a cancelled booking that still has a spa appointment on the books which is
     * already paid for (an order {@code PAID}) - one sentence per appointment, or null when there is
     * none. Read straight from the data each time rather than stored, so it stays true until staff
     * deal with it (refund, then cancel the appointment) and disappears by itself afterwards. Only
     * {@code BOOKED} appointments count: a {@code COMPLETED} one was delivered and paid, and there
     * is nothing left to decide about it.
     */
    @Transactional(readOnly = true)
    public String paidLeftBehind(String bookingId) {
        List<String> notes = new ArrayList<>();
        for (SpaAppointmentEntity appointment : spaAppointmentRepository.findByBookingId(bookingId)) {
            if (appointment.getStatus() != SpaAppointmentStatus.BOOKED) {
                continue;
            }
            linkedOrders(appointment).stream().filter(o -> o.getStatus() == OrderStatus.PAID).findFirst().ifPresent(order ->
                    notes.add("The spa appointment on " + appointment.getDate() + " at " + appointment.getStartTime().toString().substring(0, 5)
                            + " is already paid (order #" + order.getNumber() + ") and was left as it is - refund it and cancel the appointment by hand if it should not go ahead."));
        }
        return notes.isEmpty() ? null : String.join(" ", notes);
    }

    /** The appointment's own order plus its top-up orders (V144). */
    private List<OrderEntity> linkedOrders(SpaAppointmentEntity appointment) {
        List<String> ids = new ArrayList<>();
        if (appointment.getOrderId() != null) {
            ids.add(appointment.getOrderId());
        }
        spaAppointmentOrderRepository.findBySpaAppointmentId(appointment.getId()).stream().map(SpaAppointmentOrderEntity::getOrderId).forEach(ids::add);
        return ids.isEmpty() ? List.of() : orderRepository.findAllById(ids);
    }

    /** The staff member behind the call, or null for the import and the sweep (the column is a foreign key to User). */
    private static String staffActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof StaffPrincipal staff ? staff.id() : null;
    }

    /** Attributed to the caller when there is one (staff, SiteMinder), otherwise to the system - the sweep has no principal. */
    private void record(AuditAction action, AuditEntityType entityType, String entityId, String summary) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof StaffPrincipal || principal instanceof IntegrationPrincipal) {
            auditLogService.record(action, entityType, entityId, summary);
        } else {
            auditLogService.recordSystemAction(action, entityType, entityId, summary);
        }
    }
}
