package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.security.IntegrationPrincipal;
import com.sunsetbeach.security.StaffPrincipal;
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
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;

    public SpaBookingCancellationService(
            SpaAppointmentRepository spaAppointmentRepository,
            OrderRepository orderRepository,
            OrderService orderService,
            AuditLogService auditLogService) {
        this.spaAppointmentRepository = spaAppointmentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
    }

    /** The clean-up for a booking that was just cancelled; returns {@link #paidLeftBehind}'s text, or null. */
    @Transactional
    public String onBookingCancelled(BookingEntity booking) {
        List<SpaAppointmentEntity> appointments = spaAppointmentRepository.findByBookingId(booking.getId());
        String actorUserId = staffActorId();
        for (SpaAppointmentEntity appointment : appointments) {
            if (appointment.getStatus() != SpaAppointmentStatus.BOOKED) {
                continue;
            }
            OrderEntity order = linkedOrder(appointment);
            if (order != null && order.getStatus() == OrderStatus.PAID) {
                continue; // paid: left as it is, reported by paidLeftBehind
            }

            OrderService.SpaLineCleanup cleanup =
                    order != null ? orderService.removeUnpaidSpaLines(order.getId()) : OrderService.SpaLineCleanup.empty();
            appointment.setStatus(SpaAppointmentStatus.CANCELLED);
            appointment.setCancelledByUserId(actorUserId);
            appointment.setCancelReason(CANCEL_REASON);
            spaAppointmentRepository.saveAndFlush(appointment);

            String lines = cleanup.removedLines().isEmpty()
                    ? ""
                    : "; took " + String.join(", ", cleanup.removedLines()) + " off order #" + order.getNumber()
                            + (cleanup.orderCancelled() ? " (order cancelled, nothing else was on it)" : "");
            record(
                    AuditAction.SPA_APPOINTMENT_STATUS_CHANGED,
                    AuditEntityType.SPA_APPOINTMENT,
                    appointment.getId(),
                    "Spa appointment " + appointment.getId() + " marked cancelled - " + booking.getGuestName() + "'s booking was cancelled" + lines);
        }
        return paidLeftBehind(booking.getId());
    }

    /**
     * The warning for a cancelled booking that still has a spa appointment on the books which is
     * already paid for (order {@code PAID}) - one sentence per appointment, or null when there is
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
            OrderEntity order = linkedOrder(appointment);
            if (order != null && order.getStatus() == OrderStatus.PAID) {
                notes.add("The spa appointment on " + appointment.getDate() + " at " + appointment.getStartTime().toString().substring(0, 5)
                        + " is already paid (order #" + order.getNumber() + ") and was left as it is - refund it and cancel the appointment by hand if it should not go ahead.");
            }
        }
        return notes.isEmpty() ? null : String.join(" ", notes);
    }

    private OrderEntity linkedOrder(SpaAppointmentEntity appointment) {
        return appointment.getOrderId() != null ? orderRepository.findById(appointment.getOrderId()).orElse(null) : null;
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
