package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.OverstayChargeEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.OverstayChargeRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Charges the nights a {@code CHECKED_IN} guest stays past the booking's {@code checkOut} to the booking
 * (owner's decision, October 2026; before it they were only counted, at ฿0 and later at an estimate, in the
 * reports). A night is a row ({@link OverstayChargeEntity}) priced by {@link RoomNightPricing} - the price grid's
 * manual price for the date, else the room type's base price, the same function every booking night is priced
 * by - and frozen when it accrues. Each accrual is posted to the ledger the moment it happens, dated that
 * night ({@link LedgerService#postOverstayCharge}): Dr Guest Ledger, Cr Room Revenue + VAT, the way a POS room
 * charge is, so a period's Z120 holds the room revenue Z410 and the manager report count for the same nights.
 * The folio ({@link BookingFolioCalculator}) adds the active rows as its own "Overstay" line.
 *
 * <p><b>The nights:</b> {@code [checkOut, today]} while the guest is still checked in and the booking is not
 * cancelled - the same nights {@link OverstayRule} counts, tonight included (the room is theirs tonight). Which
 * is why a guest checking out on a day after their check-out is not charged that day's night
 * ({@link #settleAtCheckOut}): they did not sleep it.
 *
 * <p><b>Idempotent and self-correcting.</b> {@link #reconcile} compares the nights that should be charged with the
 * rows and does the difference - add a missing night, void one that should not be charged any more - so running
 * it twice, from the sweep, a checkout, a payment or a folio read, does nothing the second time. (booking, night)
 * is unique in the database, and the booking row is locked while it runs, so two callers cannot both add the
 * same night. A night that stopped being an overstay night - the stay was extended over it, which makes it an
 * ordinary night of the booking priced by the extension - is <i>voided</i>: its ledger entry is reversed (never
 * edited) and the row kept, so there is no double charge and no double revenue. Cancelled and no-show bookings
 * get no overstay; a checked-out booking keeps the nights its guest actually stayed and gets no new ones.
 *
 * <p>The sweep runs every few minutes and once at start-up, so nights missed while the backend was down - and the
 * nights of guests already overdue when this shipped - appear in their balances by themselves.
 */
@Service
public class OverstayChargeService {

    private static final Logger log = LoggerFactory.getLogger(OverstayChargeService.class);

    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final RoomRepository roomRepository;
    private final RoomNightPricing pricing;
    private final OverstayChargeRepository chargeRepository;
    private final LedgerService ledgerService;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public OverstayChargeService(
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomNightPricing pricing,
            OverstayChargeRepository chargeRepository,
            LedgerService ledgerService,
            Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${app.overstay.sweep-enabled:true}") boolean sweepEnabled) {
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.pricing = pricing;
        this.chargeRepository = chargeRepository;
        this.ledgerService = ledgerService;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.sweepEnabled = sweepEnabled;
    }

    private final boolean sweepEnabled;

    // --- Entry points -------------------------------------------------------------------------

    /** Brings one booking's overstay charges in line with its stay now. Safe to call as often as you like. */
    @Transactional
    public void reconcile(String bookingId) {
        BookingEntity booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        reconcile(booking, false);
    }

    /**
     * The guest is being checked out (the booking row is already locked by the caller): charge the nights they
     * slept - up to last night - and release tonight's, which accrued when the day began but which they will not
     * sleep. Run before the booking's folio is read for the check-out.
     */
    @Transactional
    public void settleAtCheckOut(BookingEntity booking) {
        reconcile(booking, true);
    }

    /** What tonight's overstay night is charged at (0 if none): the part of the balance a check-out today releases. */
    @Transactional(readOnly = true)
    public BigDecimal tonightsCharge(String bookingId) {
        LocalDate today = LocalDate.now(clock);
        return chargeRepository.findByBookingIdAndVoidedAtIsNull(bookingId).stream()
                .filter(c -> c.getDate().equals(today))
                .map(OverstayChargeEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** The sweep: every checked-in guest past check-out, plus any booking still holding a charge that no longer applies. */
    public int sweep() {
        LocalDate today = LocalDate.now(clock);
        List<String> ids = new ArrayList<>();
        bookingRepository.findByOccupancyStatusAndStatusNotAndCheckOutLessThan(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED, today)
                .forEach(b -> ids.add(b.getId()));
        // A booking that stopped being an overstay (extended, cancelled) but still has an active charge.
        chargeRepository.findBookingIdsWithActiveChargeNotCheckedOut(OccupancyStatus.CHECKED_OUT).stream().filter(id -> !ids.contains(id)).forEach(ids::add);
        int done = 0;
        for (String id : ids) {
            try {
                transaction.executeWithoutResult(status -> reconcile(id));
                done++;
            } catch (RuntimeException e) {
                log.error("Overstay charges for booking {} could not be reconciled; the next sweep tries again", id, e);
            }
        }
        return done;
    }

    @Scheduled(fixedDelayString = "${app.overstay.sweep-interval-ms:300000}", initialDelayString = "${app.overstay.sweep-initial-delay-ms:60000}")
    void scheduledSweep() {
        if (sweepEnabled) {
            sweep();
        }
    }

    // --- The work -----------------------------------------------------------------------------

    private void reconcile(BookingEntity booking, boolean checkingOut) {
        LocalDate today = LocalDate.now(clock);
        List<OverstayChargeEntity> rows = chargeRepository.findByBookingIdOrderByDateAsc(booking.getId());
        if (rows.isEmpty() && !(booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN && !booking.getCheckOut().isAfter(today.minusDays(1)))) {
            return; // nothing charged and nothing to charge: the common case costs one query
        }
        Map<LocalDate, OverstayChargeEntity> byDate = rows.stream().collect(Collectors.toMap(OverstayChargeEntity::getDate, r -> r));

        boolean noCharge = booking.getStatus() == BookingStatus.CANCELLED || booking.getOccupancyStatus() == OccupancyStatus.NO_SHOW;
        boolean inHouse = !noCharge && booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN;
        // The last night to charge: tonight while they are in the house, last night when they are leaving.
        LocalDate last = checkingOut ? today.minusDays(1) : today;
        LocalDate first = booking.getCheckOut();
        // Overdue starts the day after check-out (OverstayRule): on the departure day itself the guest is simply due out.
        boolean overdue = inHouse && first.isBefore(today);

        // 1. Void what should not be charged.
        for (OverstayChargeEntity row : rows) {
            if (!row.isActive()) {
                continue;
            }
            boolean stillOverstay = !noCharge && !row.getDate().isBefore(first) && (!inHouse || (overdue && !row.getDate().isAfter(last)));
            if (!stillOverstay) {
                voidCharge(booking, row);
            }
        }

        // 2. Charge what is missing (only a guest in the house accrues new nights).
        if (!overdue || last.isBefore(first)) {
            return;
        }
        RoomEntity room = roomOf(booking);
        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            OverstayChargeEntity row = byDate.get(d);
            if (row == null || !row.isActive()) {
                missing.add(d);
            }
        }
        if (missing.isEmpty() || room == null) {
            return;
        }
        Map<LocalDate, BigDecimal> prices = pricing.priceNights(room, missing);
        for (LocalDate night : missing) {
            OverstayChargeEntity row = byDate.get(night);
            if (row == null) {
                row = new OverstayChargeEntity();
                row.setBookingId(booking.getId());
                row.setDate(night);
            }
            row.setPrice(prices.get(night));
            row.setVoidedAt(null);
            row.setVoidEntryId(null);
            row = chargeRepository.saveAndFlush(row);
            row.setEntryId(ledgerService.postOverstayCharge(booking, row.getId(), night, row.getPrice()).getId());
            chargeRepository.saveAndFlush(row);
        }
    }

    private void voidCharge(BookingEntity booking, OverstayChargeEntity row) {
        row.setVoidEntryId(ledgerService.reverseOverstayCharge(booking, row).getId());
        row.setVoidedAt(TimestampFormat.nowUtc(clock));
        chargeRepository.saveAndFlush(row);
    }

    /** The room type the guest is in now: the last segment's. */
    private RoomEntity roomOf(BookingEntity booking) {
        return segmentRepository.findByBookingIdOrderByCheckInAsc(booking.getId()).stream()
                .max(Comparator.comparing(BookingSegmentEntity::getCheckOut))
                .flatMap(s -> roomRepository.findById(s.getRoomId()))
                .orElseGet(() -> roomRepository.findById(booking.getRoomId()).orElse(null));
    }
}
