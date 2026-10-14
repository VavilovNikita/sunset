package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The one place a booking's folio is computed - "what does this guest owe". Never stored. Lives in
 * its own bean (not in {@link BookingService}) so {@code BookingMapper} can put {@code
 * Booking.balanceDue} on every booking the API returns without a dependency cycle, and so the lists
 * can compute it for a whole page in two queries instead of two per row.
 *
 * <p>Charged: the room ({@code totalPrice} + {@code earlyDepartureFee}; nothing for a {@code
 * CANCELLED} booking, and nothing for a guest marked {@code NO_SHOW} - the stay never happened and
 * the hotel does not bill it; whatever else was charged to the booking still counts) and every {@code ROOM_CHARGE} {@link PaymentEntity}. Collected: every {@link
 * FolioPaymentEntity}, applied to the POS charges first and then to the room - so "owes for the
 * bar" clears before the room does, matching how the desk collects. A booking marked {@code PAID}
 * by hand had the rest of its room collected outside this system (an OTA prepayment, a bank
 * transfer): that remainder is {@code settledOutside}. Once folio payments cover the room, {@code
 * BookingService#recordFolioPayment} sets {@code PAID} itself, and {@code settledOutside} is zero -
 * which is why it's derived from what folio payments cover, not from {@code PAID} alone.
 */
@Component
public class BookingFolioCalculator {

    private final PaymentRepository paymentRepository;
    private final FolioPaymentRepository folioPaymentRepository;

    public BookingFolioCalculator(PaymentRepository paymentRepository, FolioPaymentRepository folioPaymentRepository) {
        this.paymentRepository = paymentRepository;
        this.folioPaymentRepository = folioPaymentRepository;
    }

    public FolioBreakdown breakdown(BookingEntity booking) {
        List<PaymentEntity> charges = paymentRepository.findByBookingIdAndMethod(booking.getId(), PaymentMethod.ROOM_CHARGE);
        List<FolioPaymentEntity> payments = folioPaymentRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        return build(booking, charges, payments);
    }

    public BigDecimal balanceDue(BookingEntity booking) {
        return breakdown(booking).balanceDue();
    }

    /**
     * {@code Booking.owes}: whether a positive {@code balanceDue} is a debt that has already fallen
     * due, as opposed to money that will be collected for a stay that has not started. True for a
     * guest in the house or gone ({@code CHECKED_IN} - an overdue one included - or {@code
     * CHECKED_OUT}), for a {@code CANCELLED} booking (what is left on it is charges, the room is
     * not owed) and for a stay whose dates are over; false for a no-show with nothing else charged
     * (the balance is zero) and for a booking that has not arrived yet, however unpaid.
     */
    public static boolean debtHasFallenDue(BookingEntity booking, BigDecimal balanceDue, java.time.LocalDate today) {
        if (balanceDue.signum() <= 0) {
            return false;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return true;
        }
        return switch (booking.getOccupancyStatus()) {
            case CHECKED_IN, CHECKED_OUT -> true;
            case EXPECTED, NO_SHOW -> booking.getCheckOut().isBefore(today);
        };
    }

    /** {@link #breakdown} for many bookings at once: one query for the POS charges, one for the payments. */
    public Map<String, FolioBreakdown> breakdowns(Collection<BookingEntity> bookings) {
        if (bookings.isEmpty()) {
            return Map.of();
        }
        List<String> ids = bookings.stream().map(BookingEntity::getId).toList();
        Map<String, List<PaymentEntity>> chargesByBooking =
                paymentRepository.findByBookingIdInAndMethod(ids, PaymentMethod.ROOM_CHARGE).stream()
                        .collect(Collectors.groupingBy(PaymentEntity::getBookingId));
        Map<String, List<FolioPaymentEntity>> paymentsByBooking = folioPaymentRepository.findByBookingIdIn(ids).stream()
                .collect(Collectors.groupingBy(FolioPaymentEntity::getBookingId));
        Map<String, FolioBreakdown> result = new HashMap<>();
        for (BookingEntity booking : bookings) {
            result.put(
                    booking.getId(),
                    build(booking, chargesByBooking.getOrDefault(booking.getId(), List.of()), paymentsByBooking.getOrDefault(booking.getId(), List.of())));
        }
        return result;
    }

    private static FolioBreakdown build(BookingEntity booking, List<PaymentEntity> charges, List<FolioPaymentEntity> payments) {
        BigDecimal roomChargesGross = charges.stream().map(PaymentEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = payments.stream().map(FolioPaymentEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FolioBreakdown(
                booking.getTotalPrice(), booking.getEarlyDepartureFee(), roomChargesGross, charges.size(), paid, booking.getStatus(), booking.getOccupancyStatus());
    }

    public record FolioBreakdown(
            BigDecimal stayPrice,
            BigDecimal earlyDepartureFee,
            BigDecimal roomChargesGross,
            int roomChargeCount,
            BigDecimal paid,
            BookingStatus status,
            OccupancyStatus occupancy) {

        /** The room as owed: nothing once the booking is cancelled, nothing for a no-show. */
        public BigDecimal roomOwed() {
            return status == BookingStatus.CANCELLED || occupancy == OccupancyStatus.NO_SHOW ? BigDecimal.ZERO : stayPrice.add(earlyDepartureFee);
        }

        public BigDecimal folioTotal() {
            return roomOwed().add(roomChargesGross);
        }

        public BigDecimal paidToCharges() {
            return paid.min(roomChargesGross);
        }

        public BigDecimal roomChargesOutstanding() {
            return roomChargesGross.subtract(paidToCharges());
        }

        /** Folio payments that went to the room, after the POS charges were covered. */
        public BigDecimal paidToRoom() {
            return paid.subtract(paidToCharges()).min(roomOwed());
        }

        public BigDecimal settledOutside() {
            return status == BookingStatus.PAID ? roomOwed().subtract(paidToRoom()) : BigDecimal.ZERO;
        }

        public BigDecimal balanceDue() {
            return folioTotal().subtract(paid).subtract(settledOutside()).max(BigDecimal.ZERO);
        }

        public BigDecimal creditBalance() {
            return paid.subtract(folioTotal()).max(BigDecimal.ZERO);
        }

        /** True once folio payments alone cover the whole room - what auto-PAID waits for. */
        public boolean roomCoveredByPayments() {
            return roomOwed().signum() > 0 && paidToRoom().compareTo(roomOwed()) >= 0;
        }

        /**
         * The same folio as if the booking were not yet {@code PAID} and not {@code CANCELLED}: what
         * is really outstanding before anyone writes the rest of the room off as settled outside
         * the system. This is the figure a change into {@code PAID} has to be paid against - read
         * from a {@code CANCELLED} booking it would otherwise be only the POS charges, and read
         * from a {@code PAID} one it would be zero.
         */
        public FolioBreakdown asUnsettled() {
            return new FolioBreakdown(stayPrice, earlyDepartureFee, roomChargesGross, roomChargeCount, paid, BookingStatus.CONFIRMED, occupancy);
        }
    }
}
