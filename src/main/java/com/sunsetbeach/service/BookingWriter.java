package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.DateText;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RelocationRateMode;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The SERIALIZABLE-isolated write path for booking creation, room-unit assignment, schedule
 * changes, and mid-stay relocation, split into its own bean (rather than a private method on
 * BookingService) so the @Transactional proxy actually applies - Spring can't intercept a
 * self-invoked call within the same instance.
 *
 * <p><b>Segments.</b> A booking's stay is a contiguous, non-overlapping sequence of
 * {@link BookingSegmentEntity} rows ("room X from date A to date B") - see
 * {@code V18__booking_segments.sql} for the full model rationale. {@link BookingEntity}'s own
 * roomId/roomUnitId/checkIn/checkOut/totalPrice are kept as denormalized mirrors of that
 * sequence (checkIn/checkOut = first/last segment's bounds, totalPrice = sum, roomId/roomUnitId
 * = the *last* segment's) by {@link #syncBookingFromSegments}, called after every write here -
 * every other reader of those columns (CSV export, emails, folio, audit summaries) keeps working
 * unchanged. Occupancy/availability and pricing run against segments, not the booking row.
 *
 * <p><b>Nightly price snapshots.</b> Each segment's {@code totalPrice} is a cached sum of its own
 * {@link BookingSegmentNightlyRateEntity} rows - one frozen price per night, first written when
 * that night becomes part of the booking (insert, an extension, or a relocation's new segment).
 * This replaced a design where every schedule-touching write recomputed a segment's *entire*
 * range from current {@code RatePlan}/{@code Room.basePrice} rates - which silently repriced
 * nights the guest had already been quoted any time a rate changed after booking and staff later
 * extended, shrank, or relocated that stay.
 *
 * <p>The rule is about whether a night's *room* changes, not merely whether a write touches it:
 * a night keeps whatever price it was last given until either (a) it's dropped from the booking
 * entirely (shrinking - its row is just deleted, nothing recomputed), (b) it's assigned to a
 * genuinely new room for the first time (extending, or a relocation's new segment - priced fresh
 * at today's rate, because there is no earlier agreement for that room to honor), or (c) a
 * manager explicitly asks to reprice it. Moving a night *out* of a segment during a relocation
 * does not delete its row - see {@link #relocate}'s javadoc - specifically so {@link
 * #undoRelocation} can restore that exact original price later instead of re-deriving one:
 * reverting a room is not a new agreement with the guest, it's correcting the record, and gets
 * the room's history back, not its current rate.
 *
 * <p><b>Room assignment comes in two shapes.</b> {@link #assignRoomUnit}/{@link #unassignRoomUnit}
 * operate on a booking's <i>sole</i> segment - convenient shorthand for the common case, a
 * booking that has never been relocated, where "the room" is unambiguous and a caller shouldn't
 * have to know or name a segment id to change it. They still reject with a clear message once
 * segments.size() > 1 (there is no way to guess which segment a bare {@code roomUnitId} would
 * apply to), directing the caller to {@link #relocate}/{@link #undoRelocation} for anything that
 * also changes dates, or to the segment-scoped siblings below for a room change that doesn't.
 * {@link #reassignSegmentRoomUnit}/{@link #unassignSegmentRoomUnit} are the general primitive
 * underneath - the same three checks (room type, active, free for the segment's own dates) and
 * the same SERIALIZABLE race-safety, just addressed at a named segment instead of implicitly
 * "the sole segment," so they work on any segment of any booking regardless of how many segments
 * it has. Neither shape supersedes the other; the whole-booking pair isn't superseded by the
 * sibling existing, it stays the convenient shorthand it always was. {@link
 * #swapSegmentRoomUnits} builds on the segment-scoped shape for the one case neither
 * single-segment call can express: moving two guests into each other's rooms in one atomic
 * write, because two sequential single-segment calls can't (the second would see the first
 * guest's segment still occupying the room it's trying to move into). See that method's own
 * javadoc for the exclusion-set reasoning it needs to correctly ignore only the two segments
 * actually swapping, not their whole parent bookings.
 *
 * <p><b>{@code updateSchedule}/{@code quoteSchedule} allow a narrower case on a multi-segment
 * booking:</b> a change that moves only the outer edge of the first or last segment (the
 * booking's own overall checkIn or checkOut, respectively) without crossing into the
 * neighboring segment. That covers the two most common front-desk requests on an
 * already-relocated stay - "one more night" (extend the last segment, same room it's already
 * in) and an earlier arrival (extend the first segment) - without forcing the roundabout
 * undo-relocate / update / re-relocate dance. Anything else against this endpoint's one
 * roomUnitId/checkIn/checkOut shape (both ends moving at once, or a date range that would cross
 * a segment boundary) is rejected in favor of {@link #relocate}/{@link #undoRelocation}. A bare
 * room change with no date change is rejected by *this* endpoint for the same reason - no single
 * segment is named - but is not rejected overall: {@link #reassignSegmentRoomUnit} is the tool
 * for exactly that case, addressed at a named segment instead of guessing which one a dateless
 * request meant. See {@link #resolveScheduleTarget}.
 */
@Service
public class BookingWriter {

    private static final String GUEST_NEEDS_A_ROOM = "A checked-in guest needs a room";

    static final String MULTI_SEGMENT_MESSAGE =
            "This booking has been split by a room relocation — change dates via relocate/undo-relocate, or one "
                    + "segment's own room via PUT .../segments/{segmentId}/room-unit.";

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final BookingSegmentNightlyRateRepository nightlyRateRepository;
    private final RoomNightPricing roomNightPricing;
    private final OverstayRule overstayRule;
    private final Clock clock;

    public BookingWriter(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            BookingSegmentNightlyRateRepository nightlyRateRepository,
            RoomNightPricing roomNightPricing,
            OverstayRule overstayRule,
            Clock clock) {
        this.clock = clock;
        this.overstayRule = overstayRule;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.nightlyRateRepository = nightlyRateRepository;
        this.roomNightPricing = roomNightPricing;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insert(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        Map<LocalDate, BigDecimal> nightlyPrices = priceNightsAtCurrentRate(room, DateRangeUtil.getNights(checkIn, checkOut));
        BookingEntity entity =
                newBookingEntity(
                        room,
                        guestName,
                        guestEmail,
                        guestPhone,
                        checkIn,
                        checkOut,
                        BookingSource.PUBLIC,
                        BookingChannel.DIRECT,
                        BookingPurpose.STANDARD,
                        adults,
                        children,
                        sumPrices(nightlyPrices));
        BookingEntity saved = bookingRepository.saveAndFlush(entity);
        saveSegment(saved.getId(), room.getId(), null, checkIn, checkOut, nightlyPrices);
        return saved;
    }

    /**
     * Staff-initiated counterpart of {@link #insert} for {@code POST /bookings/staff}: creates
     * the booking and, if {@code roomUnitId} is non-null, assigns the physical room in the same
     * SERIALIZABLE transaction. Unlike the public flow ({@code POST /bookings} followed by a
     * separate {@code PUT /bookings/{id}/room-unit}), there is no window between two writes
     * where the booking exists without the room that was asked for - either both succeed, or
     * the whole transaction rolls back and no booking is created at all.
     *
     * <p>{@code agreedTotal} (nullable) is the manager-entered price for the whole stay - see
     * {@code BookingService#createStaffBooking}. Same checks, same transaction; only the nightly
     * prices come from it instead of the current rates.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insertStaff(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingChannel channel,
            BookingPurpose purpose,
            int adults,
            int children,
            String roomUnitId,
            BigDecimal agreedTotal) {
        return insertStaff(room, guestName, guestEmail, guestPhone, checkIn, checkOut, channel, purpose, adults, children, roomUnitId, agreedTotal,
                BookingStatus.NEW, null);
    }

    /**
     * {@code status} is the booking's starting status (the caller has already restricted it to {@code NEW}
     * = Tentative or {@code CONFIRMED}) and {@code notes} the trimmed staff notes or null - both written in
     * this same transaction as the booking.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insertStaff(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingChannel channel,
            BookingPurpose purpose,
            int adults,
            int children,
            String roomUnitId,
            BigDecimal agreedTotal,
            BookingStatus status,
            String notes) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);
        // A manager-entered price replaces the rate-plan one, frozen per night the same way a
        // SiteMinder total is; null prices from the current rates as always.
        Map<LocalDate, BigDecimal> nightlyPrices =
                agreedTotal != null ? spreadEvenly(agreedTotal, nights) : priceNightsAtCurrentRate(room, nights);
        BookingEntity entity =
                newBookingEntity(
                        room, guestName, guestEmail, guestPhone, checkIn, checkOut, BookingSource.STAFF, channel, purpose, adults, children,
                        sumPrices(nightlyPrices));
        entity.setStatus(status);
        entity.setNotes(notes);

        String assignedUnitId = null;
        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            // No booking id exists yet for this not-yet-persisted stay, so there is nothing of
            // its own to exclude from the overlap check (excludeBookingId=null).
            failIfUnassignable(checkUnitAssignable(unit, room.getId(), checkIn, checkOut, null));
            entity.setRoomUnitId(unit.getId());
            assignedUnitId = unit.getId();
        }
        BookingEntity saved = bookingRepository.saveAndFlush(entity);
        saveSegment(saved.getId(), room.getId(), assignedUnitId, checkIn, checkOut, nightlyPrices);
        return saved;
    }

    /**
     * Non-mutating preview of {@link #insertStaff} for {@code POST /bookings/staff/quote}: the
     * same pricing ({@link #priceNightsAtCurrentRate}) and the same availability and unit checks,
     * so the figure the create form shows is the figure the create call would store. Never throws
     * on an unavailable result - reports it, like {@link #quoteSchedule}.
     */
    @Transactional(readOnly = true)
    public ScheduleQuote quoteStaff(RoomEntity room, LocalDate checkIn, LocalDate checkOut, String roomUnitId) {
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);
        BigDecimal totalPrice = sumPrices(priceNightsAtCurrentRate(room, nights));
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            return new ScheduleQuote(totalPrice, nights.size(), false, "Selected dates are no longer available");
        }
        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            UnitConflict conflict = checkUnitAssignable(unit, room.getId(), checkIn, checkOut, null);
            if (conflict != null) {
                return new ScheduleQuote(totalPrice, nights.size(), false, conflict.message());
            }
        }
        return new ScheduleQuote(totalPrice, nights.size(), true, null);
    }

    /**
     * SiteMinder-import counterpart of {@link #insertStaff} - same availability check in the same
     * SERIALIZABLE transaction, but the nights are frozen at {@code agreedTotal} (the price the
     * guest already agreed on the OTA, spread by {@link #spreadEvenly}) instead of this system's
     * current rates. This is the one insert that takes a price from outside: see
     * {@code SiteMinderImportService}'s class javadoc for why. {@code (source, externalReference)}
     * is unique (V123), so two concurrent imports of the same new reservation can't both land -
     * the loser fails here with a unique violation or a serialization failure.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insertExternal(
            RoomEntity room,
            String guestName,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingChannel channel,
            int adults,
            int children,
            BigDecimal agreedTotal,
            String externalReference,
            String externalChannel,
            LocalDateTime externalModifiedAt,
            LocalDateTime bookedAt) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        Map<LocalDate, BigDecimal> nightlyPrices = spreadEvenly(agreedTotal, DateRangeUtil.getNights(checkIn, checkOut));
        BookingEntity entity = newBookingEntity(
                room, guestName, null, null, checkIn, checkOut, BookingSource.SITEMINDER, channel, BookingPurpose.STANDARD, adults, children,
                sumPrices(nightlyPrices));
        entity.setStatus(BookingStatus.CONFIRMED);
        entity.setExternalReference(externalReference);
        entity.setExternalChannel(externalChannel);
        entity.setExternalModifiedAt(externalModifiedAt);
        entity.setBookedAt(bookedAt);
        BookingEntity saved = bookingRepository.saveAndFlush(entity);
        saveSegment(saved.getId(), room.getId(), null, checkIn, checkOut, nightlyPrices);
        return saved;
    }

    /**
     * Replaces every night's frozen price on a single-segment booking with {@code agreedTotal}
     * spread evenly - the SiteMinder import applying a total the guest re-agreed on the OTA. Not
     * a general reprice: unlike {@link #reprice} it rewrites past nights too, because the new
     * total is for the whole stay, not for what's left of it. Refuses a booking split by a
     * relocation, where there is no honest way to say which leg the change belongs to.
     */
    @Transactional
    public BookingEntity applyAgreedTotal(String bookingId, BigDecimal agreedTotal) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSoleSegment(bookingId);
        Map<LocalDate, BigDecimal> prices = spreadEvenly(agreedTotal, DateRangeUtil.getNights(segment.getCheckIn(), segment.getCheckOut()));
        // Every row for the segment, including any left outside its range by an earlier
        // relocation that was since undone - none of them is the agreed price any more. Flushed
        // before the insert for the same unique (segmentId, date) reason as reprice.
        nightlyRateRepository.deleteAll(nightlyRateRepository.findBySegmentIdOrderByDateAsc(segment.getId()));
        nightlyRateRepository.flush();
        saveNightlyRates(segment.getId(), prices);
        segment.setTotalPrice(sumPrices(prices));
        segmentRepository.saveAndFlush(segment);
        syncBookingFromSegments(booking, List.of(segment));
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * {@code total} split over {@code nights}: each night gets the total divided evenly, rounded
     * down to the satang, and the last night also takes the rounding remainder - so the nights
     * always sum to exactly {@code total}.
     */
    static Map<LocalDate, BigDecimal> spreadEvenly(BigDecimal total, List<LocalDate> nights) {
        if (nights.isEmpty()) {
            throw new BadRequestException("A stay needs at least one night");
        }
        BigDecimal perNight = total.divide(BigDecimal.valueOf(nights.size()), 2, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(perNight.multiply(BigDecimal.valueOf(nights.size())));
        Map<LocalDate, BigDecimal> prices = new LinkedHashMap<>();
        for (int i = 0; i < nights.size(); i++) {
            prices.put(nights.get(i), i == nights.size() - 1 ? perNight.add(remainder) : perNight);
        }
        return prices;
    }

    private BookingEntity newBookingEntity(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingSource source,
            BookingChannel channel,
            BookingPurpose purpose,
            int adults,
            int children,
            BigDecimal totalPrice) {
        BookingEntity entity = new BookingEntity();
        entity.setRoomId(room.getId());
        entity.setGuestName(guestName);
        entity.setGuestEmail(guestEmail);
        entity.setGuestPhone(guestPhone);
        entity.setCheckIn(checkIn);
        entity.setCheckOut(checkOut);
        entity.setTotalPrice(totalPrice);
        entity.setStatus(BookingStatus.NEW);
        entity.setSource(source);
        entity.setChannel(channel);
        entity.setPurpose(purpose);
        entity.setAdults(adults);
        entity.setChildren(children);
        return entity;
    }

    private BookingSegmentEntity saveSegment(
            String bookingId, String roomId, String roomUnitId, LocalDate checkIn, LocalDate checkOut, Map<LocalDate, BigDecimal> nightlyPrices) {
        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(bookingId);
        segment.setRoomId(roomId);
        segment.setRoomUnitId(roomUnitId);
        segment.setCheckIn(checkIn);
        segment.setCheckOut(checkOut);
        segment.setTotalPrice(sumPrices(nightlyPrices));
        BookingSegmentEntity saved = segmentRepository.saveAndFlush(segment);
        saveNightlyRates(saved.getId(), nightlyPrices);
        return saved;
    }

    /**
     * Assigns a physical room to a booking, guarding the same three things
     * {@code PUT /bookings/{id}/room-unit} promises: same room type, active, free for the whole
     * stay. Only legal while the booking has exactly one segment - see the class javadoc.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity assignRoomUnit(String bookingId, String roomUnitId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSoleSegment(bookingId);
        RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));

        failIfUnassignable(lockedOccupantConflict(booking, List.of(segment), segment, unit, segment.getRoomId(), Set.of()));
        failIfUnassignable(checkUnitAssignable(unit, segment.getRoomId(), segment.getCheckIn(), segment.getCheckOut(), bookingId));

        segment.setRoomUnitId(unit.getId());
        segmentRepository.saveAndFlush(segment);
        syncBookingFromSegments(booking, List.of(segment));
        return bookingRepository.saveAndFlush(booking);
    }

    /** SERIALIZABLE for a guest in the house, whom clearing a room can send back to an earlier one - see {@link #requireGuestStillHasARoom}. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity unassignRoomUnit(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSoleSegment(bookingId);
        String livedIn = OverstayRule.unitOn(booking, List.of(segment), LocalDate.now(clock), clock);
        segment.setRoomUnitId(null);
        segmentRepository.saveAndFlush(segment);
        requireGuestStillHasARoom(booking, livedIn, List.of(segment));
        syncBookingFromSegments(booking, List.of(segment));
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * Reassigns one named segment's physical room - the general primitive {@link #assignRoomUnit}
     * is a convenient shorthand for when a booking has exactly one segment (see the class
     * javadoc). Same three checks (room type, active, free for this segment's own dates) and the
     * same SERIALIZABLE race-safety, just addressed at a segment by id instead of implicitly "the
     * sole segment," so this works on any segment of any booking regardless of how many segments
     * it has. Never touches dates, segment boundaries, or any other segment's own row, and never
     * reprices: moving between two units of the *same* room type is not a new agreement - the
     * segment's already-frozen nightly rates are untouched, the same "same type, same price"
     * guarantee {@link #assignRoomUnit} already gives.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SegmentRoomUnitResult reassignSegmentRoomUnit(String bookingId, String segmentId, String roomUnitId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSegmentOnBooking(bookingId, segmentId);
        RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));

        failIfUnassignable(lockedOccupantConflict(
                booking, segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId), segment, unit, segment.getRoomId(), Set.of()));
        failIfUnassignable(checkUnitAssignable(unit, segment.getRoomId(), segment.getCheckIn(), segment.getCheckOut(), bookingId));

        segment.setRoomUnitId(unit.getId());
        segmentRepository.saveAndFlush(segment);
        List<BookingSegmentEntity> allSegments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        syncBookingFromSegments(booking, allSegments);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        return new SegmentRoomUnitResult(saved, segment, unit);
    }

    /** SERIALIZABLE for the same reason as {@link #unassignRoomUnit}. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SegmentRoomUnitResult unassignSegmentRoomUnit(String bookingId, String segmentId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSegmentOnBooking(bookingId, segmentId);
        String livedIn = OverstayRule.unitOn(
                booking, segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId), LocalDate.now(clock), clock);
        segment.setRoomUnitId(null);
        segmentRepository.saveAndFlush(segment);
        List<BookingSegmentEntity> allSegments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        requireGuestStillHasARoom(booking, livedIn, allSegments);
        syncBookingFromSegments(booking, allSegments);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        return new SegmentRoomUnitResult(saved, segment, null);
    }

    /** Result of {@link #reassignSegmentRoomUnit}/{@link #unassignSegmentRoomUnit} - the saved booking, the segment that was actually changed, and its new unit (null when unassigning). */
    public record SegmentRoomUnitResult(BookingEntity booking, BookingSegmentEntity segment, RoomUnitEntity unit) {
    }

    /**
     * Swaps this segment's physical room with {@code withSegmentId}'s, atomically - the operation
     * a drag that drops one booking's bar onto another's needs. Two sequential {@link
     * #reassignSegmentRoomUnit} calls can't do this: moving the first guest into the second
     * guest's room fails the overlap check while the second guest's own segment is still sitting
     * there. This checks both sides in one pass against a world where neither move has happened
     * yet - each segment's own dates must be free in the *other's current* room unit, excluding
     * only these two named segments (via {@link #checkUnitAssignableExcludingSegments}), not
     * their whole parent bookings and not any third segment - a segment genuinely occupying part
     * of the target unit for an overlapping sub-range still blocks the swap, the same way it
     * would block an ordinary assignment. Same room type only, checked before any availability
     * work (a 400, not a 409): the two segments' own {@code roomId} must be equal, or this is
     * rejected outright - a cross-type swap would mean either repricing both segments
     * retroactively at today's rate or leaving one guest paying the other room type's
     * already-agreed price, and this API does neither silently (see the class javadoc's "Nightly
     * price snapshots" section). A same-type swap reprices nothing - each segment keeps its own
     * already-frozen nightly rates, only {@code roomUnitId} moves. Both segments must already
     * have a physical room assigned - swapping with nothing isn't a swap, it's a single-sided
     * assignment ({@link #reassignSegmentRoomUnit}).
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SwapResult swapSegmentRoomUnits(String bookingId, String segmentId, String withSegmentId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSegmentOnBooking(bookingId, segmentId);
        if (segmentId.equals(withSegmentId)) {
            throw new BadRequestException("Cannot swap a segment with itself");
        }
        BookingSegmentEntity other = segmentRepository.findById(withSegmentId).orElseThrow(() -> new NotFoundException("Segment not found"));
        BookingEntity otherBooking =
                bookingRepository.findById(other.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));

        if (!segment.getRoomId().equals(other.getRoomId())) {
            throw new BadRequestException("These two rooms are different room types - a swap can't change either one's type");
        }
        RoomEntity room = roomRepository.findById(segment.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        if (segment.getRoomUnitId() == null || other.getRoomUnitId() == null) {
            throw new BadRequestException("Both rooms must already have a physical room assigned to swap them");
        }
        if (segment.getRoomUnitId().equals(other.getRoomUnitId())) {
            // Two stays in one room (back to back) exchanging it with each other changes nothing, yet
            // would be written to the history as "Swapped rooms". The calendar no longer offers it;
            // this keeps any other caller from recording it.
            throw new BadRequestException("These two bookings are already in the same room, so there is nothing to swap - change a booking's dates instead");
        }

        Set<String> excluded = Set.of(segment.getId(), other.getId());
        RoomUnitEntity segmentsNewUnit =
                roomUnitRepository.findById(other.getRoomUnitId()).orElseThrow(() -> new NotFoundException("Room unit not found"));
        RoomUnitEntity othersNewUnit =
                roomUnitRepository.findById(segment.getRoomUnitId()).orElseThrow(() -> new NotFoundException("Room unit not found"));

        // A guest who is in the house moves now; a booking whose current room is one of the two swapped leaves it now.
        List<BookingSegmentEntity> ownSegments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        List<BookingSegmentEntity> theirSegments = segmentRepository.findByBookingIdOrderByCheckInAsc(otherBooking.getId());
        Set<String> vacating = new HashSet<>();
        if (isCurrentSegment(ownSegments, segment)) {
            vacating.add(booking.getId());
        }
        if (isCurrentSegment(theirSegments, other)) {
            vacating.add(otherBooking.getId());
        }
        failIfUnassignable(lockedOccupantConflict(booking, ownSegments, segment, segmentsNewUnit, room.getId(), vacating));
        failIfUnassignable(lockedOccupantConflict(otherBooking, theirSegments, other, othersNewUnit, room.getId(), vacating));
        failIfUnassignable(
                checkUnitAssignableExcludingSegments(segmentsNewUnit, room.getId(), segment.getCheckIn(), segment.getCheckOut(), excluded));
        failIfUnassignable(
                checkUnitAssignableExcludingSegments(othersNewUnit, room.getId(), other.getCheckIn(), other.getCheckOut(), excluded));

        String segmentOldUnitId = segment.getRoomUnitId();
        segment.setRoomUnitId(other.getRoomUnitId());
        other.setRoomUnitId(segmentOldUnitId);
        segmentRepository.saveAndFlush(segment);
        segmentRepository.saveAndFlush(other);

        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        syncBookingFromSegments(booking, segments);
        BookingEntity savedBooking = bookingRepository.saveAndFlush(booking);

        List<BookingSegmentEntity> otherSegments = segmentRepository.findByBookingIdOrderByCheckInAsc(otherBooking.getId());
        syncBookingFromSegments(otherBooking, otherSegments);
        BookingEntity savedOtherBooking = bookingRepository.saveAndFlush(otherBooking);

        return new SwapResult(savedBooking, segment, room, savedOtherBooking, other);
    }

    /**
     * Result of {@link #swapSegmentRoomUnits} - both bookings' saved rows plus the two segments
     * that moved and the room type they share (same-type is enforced, so one {@link RoomEntity}
     * covers both sides).
     */
    public record SwapResult(BookingEntity booking, BookingSegmentEntity segment, RoomEntity room, BookingEntity otherBooking, BookingSegmentEntity otherSegment) {
    }

    /**
     * Changes a booking's dates and/or physical room in one operation - the write path behind
     * {@code PATCH /bookings/{id}/schedule}. Re-reads everything fresh (unit count, blocks,
     * overlapping segments) inside this SERIALIZABLE transaction, same as {@link #insert}, and
     * excludes this booking's own segments from every conflict check - without that, extending
     * a stay would spuriously conflict with the booking's own prior dates. Which segment the
     * change actually applies to - the sole segment, or the first/last of several - is decided
     * by {@link #resolveScheduleTarget}; only that segment's own row is mutated.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity updateSchedule(String bookingId, LocalDate newCheckIn, LocalDate newCheckOut, String roomUnitId) {
        return updateSchedule(bookingId, newCheckIn, newCheckOut, roomUnitId, false);
    }

    /**
     * {@link #updateSchedule(String, LocalDate, LocalDate, String)} with {@code earlyDeparture}: the
     * guest is leaving before the end of the stay, so a new {@code checkOut} that falls on or before
     * the start of the last segment drops the segments that begin at or after it (the nights they
     * covered are released, their frozen rates go with them) and shortens the one that remains.
     * Without it such a change is a relocation boundary crossing and is refused.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity updateSchedule(String bookingId, LocalDate newCheckIn, LocalDate newCheckOut, String roomUnitId, boolean earlyDeparture) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.isEmpty()) {
            throw new NotFoundException("Booking not found");
        }
        ScheduleTarget target = resolveScheduleTarget(segments, newCheckIn, newCheckOut, earlyDeparture);
        if (target.isRejected()) {
            if (target.badRequest()) {
                throw new BadRequestException(target.rejectionReason());
            }
            throw new ConflictException(target.rejectionReason());
        }
        BookingSegmentEntity segment = target.segment();
        LocalDate segCheckIn = target.segmentCheckIn();
        LocalDate segCheckOut = target.segmentCheckOut();
        RoomEntity room = roomRepository.findById(segment.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        if (!typeHasRoomForNewNights(segment, segCheckIn, segCheckOut, bookingId)) {
            throw new ConflictException("Selected dates are no longer available");
        }

        String livedIn = OverstayRule.unitOn(booking, segments, LocalDate.now(clock), clock);
        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            failIfUnassignable(lockedOccupantConflict(booking, segments, segment, unit, room.getId(), Set.of()));
            failIfUnassignable(checkUnitAssignable(unit, room.getId(), segCheckIn, segCheckOut, bookingId));
            segment.setRoomUnitId(unit.getId());
        } else {
            segment.setRoomUnitId(null);
        }

        // Only the nights actually being added/removed by this change are touched - see
        // repriceSchedule and the class javadoc's "Nightly price snapshots" section. Everything
        // else about the segment (its remaining nights' own frozen prices) is left exactly as it
        // was, which is the whole point: this used to recompute the segment's *entire* new range
        // from current rates, silently repricing nights nobody asked to reprice.
        ScheduleReprice reprice = repriceSchedule(segment, room, segCheckIn, segCheckOut);
        segment.setCheckIn(segCheckIn);
        segment.setCheckOut(segCheckOut);
        segment.setTotalPrice(reprice.newSegmentTotal());
        segmentRepository.saveAndFlush(segment);
        if (!target.dropped().isEmpty()) {
            // Their nightly rates go with them (ON DELETE CASCADE) - the guest has left, so there
            // is no relocation left to undo.
            segmentRepository.deleteAll(target.dropped());
            segmentRepository.flush();
        }
        if (!reprice.nightsToRemove().isEmpty()) {
            // Flushed immediately, not left for Hibernate's own action ordering to sort out: a
            // single flush runs every pending insert before every pending delete regardless of
            // the order they were registered in, which would collide with the unique
            // (segmentId, date) constraint if this ever removed and re-added the same date (see
            // reprice's identical delete-then-insert-the-same-dates shape, where it does).
            nightlyRateRepository.deleteBySegmentIdAndDateIn(segment.getId(), reprice.nightsToRemove());
            nightlyRateRepository.flush();
        }
        if (!reprice.nightsToAdd().isEmpty()) {
            saveNightlyRates(segment.getId(), reprice.addedPrices());
        }

        // Unlike relocate/undoRelocation (whose split point is always interior, so the booking's
        // overall checkIn/checkOut never move, making an assert-then-sync order the meaningful
        // check), a schedule change to an outer segment deliberately moves that overall bound -
        // sync must run first so the booking mirrors the new span, and the assert afterward is a
        // defense-in-depth check that the untouched segments still line up, not a real gap risk
        // (this method only ever moves the one edge the booking itself owns).
        List<BookingSegmentEntity> updated = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        syncBookingFromSegments(booking, updated);
        assertContinuity(booking, updated);
        if (roomUnitId == null) {
            requireGuestStillHasARoom(booking, livedIn, updated);
        }
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * Read-only, advisory preview for {@code POST /bookings/{id}/schedule/quote} - runs the
     * exact same checks as {@link #updateSchedule} (sharing {@link #checkUnitAssignable}/
     * {@link #isRangeAvailable}/{@link #resolveScheduleTarget}/{@link #repriceSchedule} so the
     * two cannot drift apart), but never writes and never throws on an unavailable result: it
     * reports {@code available=false} with a reason instead - "no" is a valid answer to a
     * preview, including "no, that would touch more than one segment."
     */
    @Transactional(readOnly = true)
    public ScheduleQuote quoteSchedule(String bookingId, LocalDate newCheckIn, LocalDate newCheckOut, String roomUnitId) {
        return quoteSchedule(bookingId, newCheckIn, newCheckOut, roomUnitId, false);
    }

    /** {@link #quoteSchedule(String, LocalDate, LocalDate, String)} for {@code updateSchedule}'s {@code earlyDeparture} variant. */
    @Transactional(readOnly = true)
    public ScheduleQuote quoteSchedule(String bookingId, LocalDate newCheckIn, LocalDate newCheckOut, String roomUnitId, boolean earlyDeparture) {
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.isEmpty()) {
            throw new NotFoundException("Booking not found");
        }
        ScheduleTarget target = resolveScheduleTarget(segments, newCheckIn, newCheckOut, earlyDeparture);
        if (target.isRejected()) {
            return new ScheduleQuote(BigDecimal.ZERO, 0, false, target.rejectionReason());
        }
        BookingSegmentEntity segment = target.segment();
        LocalDate segCheckIn = target.segmentCheckIn();
        LocalDate segCheckOut = target.segmentCheckOut();
        RoomEntity room = roomRepository.findById(segment.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        ScheduleReprice reprice = repriceSchedule(segment, room, segCheckIn, segCheckOut);
        BigDecimal othersTotal = segments.stream()
                .filter(s -> !s.getId().equals(segment.getId()) && !target.dropped().contains(s))
                .map(BookingSegmentEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice = othersTotal.add(reprice.newSegmentTotal());
        int nights = DateRangeUtil.getNights(newCheckIn, newCheckOut).size();

        if (!typeHasRoomForNewNights(segment, segCheckIn, segCheckOut, bookingId)) {
            return new ScheduleQuote(totalPrice, nights, false, "Selected dates are no longer available");
        }

        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (roomUnitId == null && leavesGuestWithoutRoom(booking, segments, target)) {
            return new ScheduleQuote(totalPrice, nights, false, GUEST_NEEDS_A_ROOM);
        }
        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            UnitConflict conflict = occupantConflict(booking, segments, segment, unit, room.getId(), Set.of());
            if (conflict == null) {
                conflict = checkUnitAssignable(unit, room.getId(), segCheckIn, segCheckOut, bookingId);
            }
            if (conflict != null) {
                return new ScheduleQuote(totalPrice, nights, false, conflict.message());
            }
        }

        return new ScheduleQuote(totalPrice, nights, true, null);
    }

    /**
     * Decides which segment (if any) a {@code PATCH /bookings/{id}/schedule} change
     * unambiguously applies to, given the request's single overall {@code newCheckIn}/
     * {@code newCheckOut}/{@code roomUnitId}:
     *
     * <ul>
     *   <li>A single-segment booking always targets that segment - both dates may move freely,
     *       exactly like before segments existed.</li>
     *   <li>A multi-segment booking targets the <b>first</b> segment when only {@code newCheckIn}
     *       moves (an earlier/later arrival) - or the <b>last</b> segment when only
     *       {@code newCheckOut} moves (staying extra nights, the most common front-desk request
     *       on an already-relocated stay). Either way the moving edge must not cross into the
     *       neighboring segment - shrinking or stretching past a relocation boundary isn't a
     *       schedule change, it's an undo-relocation or a new relocation.</li>
     *   <li>Everything else - both dates moving at once, or neither moving (a bare room change,
     *       which segment that would apply to is not stated anywhere in the request) - has no
     *       single segment this endpoint's shape can name. Both-ends-moving is rejected in favor
     *       of {@link #relocate}/{@link #undoRelocation}; a bare room change is rejected in favor
     *       of {@link #reassignSegmentRoomUnit}, which takes a segment id instead of guessing
     *       one from a dateless request.</li>
     * </ul>
     */
    private ScheduleTarget resolveScheduleTarget(
            List<BookingSegmentEntity> sortedSegments, LocalDate newCheckIn, LocalDate newCheckOut, boolean earlyDeparture) {
        if (sortedSegments.size() == 1) {
            return ScheduleTarget.of(sortedSegments.get(0), newCheckIn, newCheckOut);
        }
        BookingSegmentEntity first = sortedSegments.get(0);
        BookingSegmentEntity last = sortedSegments.get(sortedSegments.size() - 1);
        boolean checkInChanged = !newCheckIn.equals(first.getCheckIn());
        boolean checkOutChanged = !newCheckOut.equals(last.getCheckOut());

        if (checkInChanged && !checkOutChanged) {
            if (!newCheckIn.isBefore(first.getCheckOut())) {
                return ScheduleTarget.badRequest("New check-in must stay before " + DateText.date(first.getCheckOut())
                        + " - crossing into the next segment isn't a schedule change, it's a relocation.");
            }
            return ScheduleTarget.of(first, newCheckIn, first.getCheckOut());
        }
        if (checkOutChanged && !checkInChanged) {
            if (earlyDeparture && newCheckOut.isBefore(last.getCheckOut()) && !newCheckOut.isAfter(last.getCheckIn())) {
                // The guest leaves before the last room's first night, so that room is never used:
                // its segment (and any other starting on or after the new departure) goes, and the
                // segment holding the new last night is shortened.
                List<BookingSegmentEntity> kept = sortedSegments.stream().filter(s -> s.getCheckIn().isBefore(newCheckOut)).toList();
                List<BookingSegmentEntity> dropped = sortedSegments.stream().filter(s -> !s.getCheckIn().isBefore(newCheckOut)).toList();
                BookingSegmentEntity newLast = kept.get(kept.size() - 1);
                return new ScheduleTarget(newLast, newLast.getCheckIn(), newCheckOut, null, false, dropped);
            }
            if (!newCheckOut.isAfter(last.getCheckIn())) {
                return ScheduleTarget.badRequest("New check-out must stay after " + DateText.date(last.getCheckIn())
                        + " - crossing into the previous segment isn't a schedule change, it's an undo-relocation.");
            }
            return ScheduleTarget.of(last, last.getCheckIn(), newCheckOut);
        }
        return ScheduleTarget.rejected(MULTI_SEGMENT_MESSAGE);
    }

    /** Result of {@link #resolveScheduleTarget} - either a segment to change and its new bounds, or a reason it can't be resolved. */
    private record ScheduleTarget(
            BookingSegmentEntity segment,
            LocalDate segmentCheckIn,
            LocalDate segmentCheckOut,
            String rejectionReason,
            boolean badRequest,
            List<BookingSegmentEntity> dropped) {
        static ScheduleTarget of(BookingSegmentEntity segment, LocalDate checkIn, LocalDate checkOut) {
            return new ScheduleTarget(segment, checkIn, checkOut, null, false, List.of());
        }

        static ScheduleTarget rejected(String reason) {
            return new ScheduleTarget(null, null, null, reason, false, List.of());
        }

        static ScheduleTarget badRequest(String reason) {
            return new ScheduleTarget(null, null, null, reason, true, List.of());
        }

        boolean isRejected() {
            return segment == null;
        }
    }

    /**
     * Whether the type can take the nights a schedule change adds to {@code segment}. A night the
     * segment already holds is not asked about: keeping it changes no count, so a room change or a
     * shortening inside an oversold type must not be refused for it.
     */
    private boolean typeHasRoomForNewNights(BookingSegmentEntity segment, LocalDate newCheckIn, LocalDate newCheckOut, String bookingId) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(segment.getRoomId());
        // The new nights before the ones already held, and the new nights after them (both may be empty).
        LocalDate beforeEnd = newCheckOut.isBefore(segment.getCheckIn()) ? newCheckOut : segment.getCheckIn();
        LocalDate afterStart = newCheckIn.isAfter(segment.getCheckOut()) ? newCheckIn : segment.getCheckOut();
        boolean before = !newCheckIn.isBefore(beforeEnd)
                || isRangeAvailable(segment.getRoomId(), unitCount, newCheckIn, beforeEnd, bookingId);
        return before
                && (!afterStart.isBefore(newCheckOut)
                        || isRangeAvailable(segment.getRoomId(), unitCount, afterStart, newCheckOut, bookingId));
    }

    /**
     * What a schedule change (extend/shrink one edge of {@code segment} to {@code newCheckIn}/
     * {@code newCheckOut}) would do to that segment's price - shared by {@link #updateSchedule}
     * and {@link #quoteSchedule} so the two price a change identically, the same reasoning
     * {@link #resolveScheduleTarget} is already shared for. Computed as a set difference against
     * the segment's *current* nights, not just its edge (correct even in the never-actually-
     * reachable case of a request that doesn't overlap the old range at all): nights present in
     * both old and new are untouched, nights only in the new range are priced at today's rate,
     * nights only in the old range are dropped with no repricing of anything else.
     */
    private ScheduleReprice repriceSchedule(BookingSegmentEntity segment, RoomEntity room, LocalDate newCheckIn, LocalDate newCheckOut) {
        List<LocalDate> oldNights = DateRangeUtil.getNights(segment.getCheckIn(), segment.getCheckOut());
        List<LocalDate> newNights = DateRangeUtil.getNights(newCheckIn, newCheckOut);
        Set<LocalDate> oldSet = new HashSet<>(oldNights);
        Set<LocalDate> newSet = new HashSet<>(newNights);
        List<LocalDate> nightsToAdd = newNights.stream().filter(n -> !oldSet.contains(n)).toList();
        List<LocalDate> nightsToRemove = oldNights.stream().filter(n -> !newSet.contains(n)).toList();

        Map<LocalDate, BigDecimal> existingRates = existingRatesByDate(segment.getId());
        BigDecimal removedSum = nightsToRemove.stream().map(existingRates::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<LocalDate, BigDecimal> addedPrices = priceNightsAtCurrentRate(room, nightsToAdd);
        BigDecimal newTotal = segment.getTotalPrice().subtract(removedSum).add(sumPrices(addedPrices));
        return new ScheduleReprice(newTotal, nightsToAdd, addedPrices, nightsToRemove);
    }

    /** Result of {@link #repriceSchedule}. */
    private record ScheduleReprice(
            BigDecimal newSegmentTotal, List<LocalDate> nightsToAdd, Map<LocalDate, BigDecimal> addedPrices, List<LocalDate> nightsToRemove) {
    }

    /**
     * What a relocation would do, worked out once and shared by {@link #relocate} and
     * {@link #quoteRelocation} so the number a quote shows and the number an apply charges cannot
     * drift apart. {@code keepPrices}/{@code newPrices} are the per-night prices of the nights that
     * move ({@code movedNights}) under each rate mode; {@code newPrices} is null within one room
     * type, where the price can never change.
     */
    private record RelocationPlan(
            BookingSegmentEntity target,
            RoomEntity oldRoom,
            RoomEntity newRoom,
            boolean sameType,
            boolean wholeSegment,
            List<LocalDate> movedNights,
            Map<LocalDate, BigDecimal> keepPrices,
            Map<LocalDate, BigDecimal> newPrices,
            BigDecimal retainedOnOldSegment,
            BigDecimal othersTotal,
            BigDecimal currentTotal) {

        BigDecimal totalWith(Map<LocalDate, BigDecimal> prices) {
            return othersTotal.add(retainedOnOldSegment).add(sumPrices(prices));
        }
    }

    private RelocationPlan planRelocation(List<BookingSegmentEntity> segments, LocalDate effectiveDate, String newRoomId) {
        BookingSegmentEntity target = findSegmentContaining(segments, effectiveDate)
                .orElseThrow(() -> new BadRequestException("effectiveDate must fall within this booking's stay"));
        RoomEntity oldRoom = roomRepository.findById(target.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomEntity newRoom = roomRepository.findById(newRoomId).orElseThrow(() -> new NotFoundException("Room not found"));
        boolean sameType = oldRoom.getId().equals(newRoom.getId());
        // On the segment's first night nothing is left behind to split off: the whole segment changes room.
        boolean whole = effectiveDate.equals(target.getCheckIn());
        List<LocalDate> moved = DateRangeUtil.getNights(effectiveDate, target.getCheckOut());

        Map<LocalDate, BigDecimal> existing = existingRatesByDate(target.getId());
        List<LocalDate> missing = moved.stream().filter(n -> !existing.containsKey(n)).toList();
        Map<LocalDate, BigDecimal> fallback = missing.isEmpty() ? Map.of() : priceNightsAtCurrentRate(oldRoom, missing);
        Map<LocalDate, BigDecimal> keep = new LinkedHashMap<>();
        for (LocalDate night : moved) {
            keep.put(night, existing.getOrDefault(night, fallback.get(night)));
        }

        Map<LocalDate, BigDecimal> fresh = null;
        if (!sameType) {
            // Only nights from today forward take the new rate; a night already slept keeps its agreed price.
            LocalDate today = LocalDate.now(clock);
            List<LocalDate> repriceable = moved.stream().filter(n -> !n.isBefore(today)).toList();
            Map<LocalDate, BigDecimal> current = repriceable.isEmpty() ? Map.of() : priceNightsAtCurrentRate(newRoom, repriceable);
            fresh = new LinkedHashMap<>();
            for (LocalDate night : moved) {
                fresh.put(night, current.getOrDefault(night, keep.get(night)));
            }
        }

        BigDecimal retained = whole ? BigDecimal.ZERO : target.getTotalPrice().subtract(sumPrices(keep));
        BigDecimal others = segments.stream()
                .filter(s -> !s.getId().equals(target.getId()))
                .map(BookingSegmentEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelocationPlan(target, oldRoom, newRoom, sameType, whole, moved, keep, fresh, retained, others,
                others.add(target.getTotalPrice()));
    }

    /**
     * Whether the destination type can take the moved nights. Always true within one type: the guest
     * already holds one of its rooms for those nights, so the move changes no count - an oversold
     * type (overstaying guests) used to refuse a move onto its own free room. The room itself is
     * still checked ({@link #checkUnitAssignable}).
     */
    private boolean typeHasRoomFor(RelocationPlan plan, LocalDate effectiveDate, String bookingId) {
        if (plan.sameType()) {
            return true;
        }
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(plan.newRoom().getId());
        return isRangeAvailable(plan.newRoom().getId(), unitCount, effectiveDate, plan.target().getCheckOut(), bookingId);
    }

    /**
     * The rate mode a request ends up with: within one room type always {@code KEEP_RATE} (asking
     * for {@code NEW_RATE} there is a 400 - re-rating nights is {@code reprice}); across types the
     * request's choice, {@code NEW_RATE} when it made none.
     */
    private static RelocationRateMode resolveRateMode(RelocationPlan plan, RelocationRateMode requested) {
        if (plan.sameType()) {
            if (requested == RelocationRateMode.NEW_RATE) {
                throw new BadRequestException(
                        "The new room is of the same type, so the price does not change. To re-rate the nights use reprice.");
            }
            return RelocationRateMode.KEEP_RATE;
        }
        return requested == null ? RelocationRateMode.NEW_RATE : requested;
    }

    /**
     * Moves the guest to {@code newRoomId}/{@code newRoomUnitId} from {@code effectiveDate} to the
     * end of the segment covering it. Two shapes, by where the date falls:
     *
     * <ul>
     *   <li><b>After the segment's first night</b> - the segment is split. The existing room keeps
     *       {@code [oldCheckIn, effectiveDate)} at whatever it was already priced, and a new segment
     *       for {@code [effectiveDate, oldCheckOut)} takes the new room. The truncated segment's own
     *       rows for the nights it gives up are deliberately left in place, not deleted, even
     *       though they now fall outside its range - see {@link #undoRelocation}'s comment for why
     *       keeping them is what makes undoing this relocation later a real undo.</li>
     *   <li><b>On the segment's first night</b> (a guest arriving today, or a segment that begins
     *       where an earlier move ended) - nothing is left to split off, so the whole segment
     *       changes room/unit/type in place. There is no boundary created and so nothing for
     *       undo to merge.</li>
     * </ul>
     *
     * <p><b>Price.</b> {@link RelocationRateMode#KEEP_RATE}: every moved night keeps the exact
     * frozen price it had (a hand-typed or SiteMinder price included), so the booking's total does
     * not change. {@link RelocationRateMode#NEW_RATE}: nights from today forward are priced at the
     * new type's current rate by {@code RoomNightPricing} (a night already slept keeps its price);
     * only across room types. {@code KEEP_RATE} across types needs {@code mayKeepRate}.
     *
     * <p>Same three checks as {@link #assignRoomUnit} against the *new* room/unit for the moved
     * dates, same SERIALIZABLE race-safety as {@link #insert}.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RelocationResult relocate(
            String bookingId,
            LocalDate effectiveDate,
            String newRoomId,
            String newRoomUnitId,
            RelocationRateMode requestedMode,
            boolean mayKeepRate) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        RelocationPlan plan = planRelocation(segments, effectiveDate, newRoomId);
        BookingSegmentEntity target = plan.target();
        RoomEntity newRoom = plan.newRoom();
        RelocationRateMode mode = resolveRateMode(plan, requestedMode);
        if (!plan.sameType() && mode == RelocationRateMode.KEEP_RATE && !mayKeepRate) {
            throw new ForbiddenException("Only a manager can keep the original rate when the guest moves to another room type");
        }
        LocalDate oldCheckOut = target.getCheckOut();

        if (!typeHasRoomFor(plan, effectiveDate, bookingId)) {
            throw new ConflictException("Selected room type has no availability for that period");
        }
        RoomUnitEntity newUnit = null;
        if (newRoomUnitId != null) {
            newUnit = roomUnitRepository.findById(newRoomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            // A move planned for a later night moves nobody yet: whoever is in that room today will have left.
            if (!effectiveDate.isAfter(LocalDate.now(clock))) {
                failIfUnassignable(lockedOccupantConflict(booking, segments, target, newUnit, newRoom.getId(), Set.of()));
            }
            failIfUnassignable(checkUnitAssignable(newUnit, newRoom.getId(), effectiveDate, oldCheckOut, bookingId));
        }
        String newUnitId = newUnit != null ? newUnit.getId() : null;
        if (plan.wholeSegment() && plan.sameType() && Objects.equals(target.getRoomUnitId(), newUnitId)) {
            throw new BadRequestException("The guest is already in that room");
        }

        String oldUnitId = target.getRoomUnitId();
        String oldUnitLabel = oldUnitId != null ? roomUnitRepository.findById(oldUnitId).map(RoomUnitEntity::getLabel).orElse(null) : null;
        Map<LocalDate, BigDecimal> prices = mode == RelocationRateMode.NEW_RATE ? plan.newPrices() : plan.keepPrices();

        if (plan.wholeSegment()) {
            target.setRoomId(newRoom.getId());
            target.setRoomUnitId(newUnitId);
            if (mode == RelocationRateMode.NEW_RATE) {
                Map<LocalDate, BigDecimal> changed = new LinkedHashMap<>();
                prices.forEach((night, price) -> {
                    if (price.compareTo(plan.keepPrices().get(night)) != 0) {
                        changed.put(night, price);
                    }
                });
                // Same delete-then-flush-then-insert order as reprice, for the unique (segmentId, date).
                nightlyRateRepository.deleteAll(nightlyRateRepository.findBySegmentIdOrderByDateAsc(target.getId()).stream()
                        .filter(r -> changed.containsKey(r.getDate()))
                        .toList());
                nightlyRateRepository.flush();
                saveNightlyRates(target.getId(), changed);
                target.setTotalPrice(sumPrices(prices));
            }
            segmentRepository.saveAndFlush(target);
        } else {
            target.setCheckOut(effectiveDate);
            target.setTotalPrice(plan.retainedOnOldSegment());
            segmentRepository.saveAndFlush(target);
            // No delete here - see the javadoc: these rows are the guest's original price for these
            // nights, and keeping them (inert, outside target's own range) is what undo restores from.
            saveSegment(bookingId, newRoom.getId(), newUnitId, effectiveDate, oldCheckOut, prices);
        }

        List<BookingSegmentEntity> updated = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        assertContinuity(booking, updated);
        syncBookingFromSegments(booking, updated);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        return new RelocationResult(saved, plan.oldRoom(), oldUnitId, oldUnitLabel, newRoom, newUnit, plan.wholeSegment(), mode,
                plan.currentTotal(), saved.getTotalPrice());
    }

    /**
     * {@link #relocate}'s saved booking plus the old/new room identity, for the audit summary
     * ("from X to Y"). The last four describe a relocation only (null for an undo): whether the
     * whole segment moved, the rate mode, and the booking's total before and after.
     */
    public record RelocationResult(
            BookingEntity booking,
            RoomEntity oldRoom,
            String oldUnitId,
            String oldUnitLabel,
            RoomEntity newRoom,
            RoomUnitEntity newUnit,
            Boolean wholeSegment,
            RelocationRateMode rateMode,
            BigDecimal oldTotal,
            BigDecimal newTotal) {
    }

    /**
     * Read-only preview for {@code POST /bookings/{id}/relocate/quote}: the same {@link
     * RelocationPlan} {@link #relocate} writes from, so every figure is what an apply would
     * charge. {@code newTotal} is null within one room type.
     */
    public record RelocationQuoteResult(
            BigDecimal totalPrice,
            int nights,
            boolean available,
            String reason,
            BigDecimal currentTotal,
            boolean sameType,
            boolean wholeSegment,
            RelocationRateMode rateMode,
            boolean mayKeepRate,
            BigDecimal keepTotal,
            BigDecimal newTotal) {
    }

    @Transactional(readOnly = true)
    public RelocationQuoteResult quoteRelocation(
            String bookingId,
            LocalDate effectiveDate,
            String newRoomId,
            String newRoomUnitId,
            RelocationRateMode requestedMode,
            boolean mayKeepRate) {
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.isEmpty()) {
            throw new NotFoundException("Booking not found");
        }
        BigDecimal currentTotal = segments.stream().map(BookingSegmentEntity::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (findSegmentContaining(segments, effectiveDate).isEmpty()) {
            return new RelocationQuoteResult(currentTotal, 0, false, "effectiveDate must fall within this booking's stay", currentTotal,
                    true, false, RelocationRateMode.KEEP_RATE, mayKeepRate, currentTotal, null);
        }
        RelocationPlan plan = planRelocation(segments, effectiveDate, newRoomId);
        BookingSegmentEntity target = plan.target();
        BigDecimal keepTotal = plan.totalWith(plan.keepPrices());
        BigDecimal newTotal = plan.newPrices() == null ? null : plan.totalWith(plan.newPrices());
        int nights = plan.movedNights().size();
        RelocationRateMode mode;
        try {
            mode = resolveRateMode(plan, requestedMode);
        } catch (BadRequestException e) {
            return new RelocationQuoteResult(currentTotal, nights, false, e.getMessage(), currentTotal, plan.sameType(), plan.wholeSegment(),
                    RelocationRateMode.KEEP_RATE, true, keepTotal, newTotal);
        }
        BigDecimal total = mode == RelocationRateMode.NEW_RATE ? newTotal : keepTotal;
        boolean mayKeep = plan.sameType() || mayKeepRate;

        if (!typeHasRoomFor(plan, effectiveDate, bookingId)) {
            return new RelocationQuoteResult(total, nights, false, "Selected room type has no availability for that period", currentTotal,
                    plan.sameType(), plan.wholeSegment(), mode, mayKeep, keepTotal, newTotal);
        }
        String unavailable = null;
        if (newRoomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(newRoomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
            UnitConflict conflict = effectiveDate.isAfter(LocalDate.now(clock))
                    ? null
                    : occupantConflict(booking, segments, target, unit, plan.newRoom().getId(), Set.of());
            if (conflict == null) {
                conflict = checkUnitAssignable(unit, plan.newRoom().getId(), effectiveDate, target.getCheckOut(), bookingId);
            }
            if (conflict != null) {
                unavailable = conflict.message();
            }
        }
        if (unavailable == null && plan.wholeSegment() && plan.sameType() && Objects.equals(target.getRoomUnitId(), newRoomUnitId)) {
            unavailable = "The guest is already in that room";
        }
        return new RelocationQuoteResult(total, nights, unavailable == null, unavailable, currentTotal, plan.sameType(),
                plan.wholeSegment(), mode, mayKeep, keepTotal, newTotal);
    }

    /**
     * The inverse of {@link #relocate}: merges the two segments meeting at {@code splitDate}
     * back into one, keeping the *earlier* segment's room (undoing a relocation means "this move
     * didn't happen," so the room reverts to what covered that whole span before it).
     *
     * <p><b>Pricing is a real restore, not a reprice.</b> An earlier version of this method
     * treated undo as symmetric with {@link #relocate}'s own "new room, new price" rule - reason
     * being, reverting a room is itself a new room assignment. That reasoning is correct for a
     * forward relocation (the guest is genuinely moving and agreeing to a new room's terms), but
     * wrong here: nothing physically happens when an undo is applied - it corrects a record or
     * reverses a decision that was just made, not a fresh agreement with the guest. Under the old
     * rule, a rate hike between the relocation and its undo would hand the guest a bill for a
     * night they never asked to be repriced, which is exactly the class of bug this whole nightly-
     * snapshot design exists to prevent. The only reason that mistake shipped once already is that
     * the test guarding this method predates nightly snapshots entirely - it was written when
     * *every* schedule-touching write repriced wholesale, so it happened to still pass under the
     * new "reprice on revert" rule too (its rates never changed mid-test) without actually
     * testing which rule was right. A green test that predates the design decision it's being
     * used to justify does not confirm that decision - it just failed to contradict it once. Get
     * an explicit test for the case that actually distinguishes them (a rate hike between
     * relocate and undo) before trusting either behavior.
     *
     * <p>Given that, undo restores {@code after}'s nights to whatever they cost under {@code
     * before}'s room *before* the relocation moved them out - {@link #relocate} deliberately
     * leaves the truncated segment's own rows in place for exactly this (see its own javadoc),
     * so the original price is normally still sitting right there under {@code before}'s segment
     * id, just outside its current {@code checkOut}; restoring it is a lookup, not a
     * recomputation. The one exception is a night whose original row genuinely isn't there
     * (a relocation from before this design existed, or some other gap) - that night alone falls
     * back to today's rate for the reverted room, the same way a never-previously-owned night
     * would; it's the best available answer once the real history is gone, not a silent
     * assumption that it never existed. Not a no-op write otherwise: the earlier room's unit/type
     * may no longer be free for the range it's about to re-absorb (someone else could have booked
     * it in the meantime, since the relocation made it look free), so this re-runs the same
     * availability check {@link #relocate} does, under the same SERIALIZABLE isolation.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RelocationResult undoRelocation(String bookingId, LocalDate splitDate) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        BookingSegmentEntity before = segments.stream().filter(s -> s.getCheckOut().equals(splitDate)).findFirst()
                .orElseThrow(() -> new BadRequestException("No relocation boundary at " + DateText.date(splitDate)));
        BookingSegmentEntity after = segments.stream().filter(s -> s.getCheckIn().equals(splitDate)).findFirst()
                .orElseThrow(() -> new BadRequestException("No relocation boundary at " + DateText.date(splitDate)));

        RoomEntity room = roomRepository.findById(before.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomEntity discardedRoom = roomRepository.findById(after.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        String discardedUnitLabel = after.getRoomUnitId() != null
                ? roomUnitRepository.findById(after.getRoomUnitId()).map(RoomUnitEntity::getLabel).orElse(null)
                : null;
        LocalDate mergedCheckOut = after.getCheckOut();

        // Undoing a move within one type changes no count (see typeHasRoomFor).
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!room.getId().equals(discardedRoom.getId()) && !isRangeAvailable(room.getId(), unitCount, splitDate, mergedCheckOut, bookingId)) {
            throw new ConflictException("Cannot undo — the earlier room type has no availability for the merged period");
        }
        if (before.getRoomUnitId() != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(before.getRoomUnitId()).orElseThrow(() -> new NotFoundException("Room unit not found"));
            // Undoing the move a guest in the house is living in puts them back into the earlier room now.
            failIfUnassignable(lockedOccupantConflict(booking, segments, after, unit, room.getId(), Set.of()));
            failIfUnassignable(checkUnitAssignable(unit, room.getId(), splitDate, mergedCheckOut, bookingId));
        }

        // Restore from before's own preserved rows wherever they exist; only a genuinely missing
        // night falls back to today's rate. "after"'s own rows are simply discarded (via
        // ON DELETE CASCADE when it's deleted below) - they priced a room this booking is
        // leaving, and that price leaves with it.
        List<LocalDate> revertingNights = DateRangeUtil.getNights(splitDate, mergedCheckOut);
        Map<LocalDate, BigDecimal> beforesOwnRates = existingRatesByDate(before.getId());
        List<LocalDate> missingNights = revertingNights.stream().filter(n -> !beforesOwnRates.containsKey(n)).toList();
        Map<LocalDate, BigDecimal> freshPricesForMissing = missingNights.isEmpty() ? Map.of() : priceNightsAtCurrentRate(room, missingNights);
        BigDecimal restoredSum = revertingNights.stream()
                .map(n -> beforesOwnRates.getOrDefault(n, freshPricesForMissing.get(n)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        before.setCheckOut(mergedCheckOut);
        before.setTotalPrice(before.getTotalPrice().add(restoredSum));
        segmentRepository.saveAndFlush(before);
        segmentRepository.delete(after);
        segmentRepository.flush();
        if (!freshPricesForMissing.isEmpty()) {
            saveNightlyRates(before.getId(), freshPricesForMissing);
        }

        List<BookingSegmentEntity> updated = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        assertContinuity(booking, updated);
        syncBookingFromSegments(booking, updated);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        return new RelocationResult(saved, discardedRoom, null, discardedUnitLabel, room,
                before.getRoomUnitId() != null ? roomUnitRepository.findById(before.getRoomUnitId()).orElse(null) : null,
                null, null, null, null);
    }

    /**
     * The one explicit, deliberate way to move an already-agreed price forward - see the class
     * javadoc's "Nightly price snapshots" section. Only ever touches nights from today onward
     * (inclusive) within the named segment; a night the guest has already stayed keeps whatever
     * was agreed for it even when this is invoked mid-stay - there is no "explicit" override for
     * the past, only for what hasn't happened yet. A segment with nothing left to reprice
     * (entirely in the past) is a legitimate no-op, not an error.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RepriceResult reprice(String bookingId, String segmentId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSegmentOnBooking(bookingId, segmentId);
        RoomEntity room = roomRepository.findById(segment.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        RepricePreview preview = previewReprice(segment, room);
        if (!preview.nightsToReprice().isEmpty()) {
            // Flushed immediately - see updateSchedule's identical comment. Reprice always
            // deletes and re-inserts the *same* dates, so without this the insert half of the
            // next flush would race the not-yet-executed delete and collide on the unique
            // (segmentId, date) constraint.
            nightlyRateRepository.deleteBySegmentIdAndDateIn(segmentId, preview.nightsToReprice());
            nightlyRateRepository.flush();
            saveNightlyRates(segmentId, preview.newPrices());
            segment.setTotalPrice(preview.newSegmentTotal());
            segmentRepository.saveAndFlush(segment);
        }

        List<BookingSegmentEntity> allSegments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        syncBookingFromSegments(booking, allSegments);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        return new RepriceResult(saved, preview.oldSegmentTotal(), preview.newSegmentTotal(), preview.nightsToReprice().size());
    }

    /** Read-only preview for {@code POST /bookings/{id}/reprice/quote} - identical computation to {@link #reprice}, never writing. */
    @Transactional(readOnly = true)
    public RepriceResult quoteReprice(String bookingId, String segmentId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSegmentOnBooking(bookingId, segmentId);
        RoomEntity room = roomRepository.findById(segment.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        RepricePreview preview = previewReprice(segment, room);
        return new RepriceResult(booking, preview.oldSegmentTotal(), preview.newSegmentTotal(), preview.nightsToReprice().size());
    }

    private RepricePreview previewReprice(BookingSegmentEntity segment, RoomEntity room) {
        LocalDate today = LocalDate.now(clock);
        LocalDate repriceFrom = today.isAfter(segment.getCheckIn()) ? today : segment.getCheckIn();
        if (!repriceFrom.isBefore(segment.getCheckOut())) {
            return new RepricePreview(List.of(), Map.of(), segment.getTotalPrice(), segment.getTotalPrice());
        }
        List<LocalDate> nightsToReprice = DateRangeUtil.getNights(repriceFrom, segment.getCheckOut());
        Map<LocalDate, BigDecimal> existingRates = existingRatesByDate(segment.getId());
        BigDecimal oldSum = nightsToReprice.stream().map(existingRates::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<LocalDate, BigDecimal> newPrices = priceNightsAtCurrentRate(room, nightsToReprice);
        BigDecimal oldSegmentTotal = segment.getTotalPrice();
        BigDecimal newSegmentTotal = oldSegmentTotal.subtract(oldSum).add(sumPrices(newPrices));
        return new RepricePreview(nightsToReprice, newPrices, oldSegmentTotal, newSegmentTotal);
    }

    private record RepricePreview(
            List<LocalDate> nightsToReprice, Map<LocalDate, BigDecimal> newPrices, BigDecimal oldSegmentTotal, BigDecimal newSegmentTotal) {
    }

    /** Result of {@link #reprice}/{@link #quoteReprice} - the segment's own before/after total (not the whole booking's), plus how many nights were touched. */
    public record RepriceResult(BookingEntity booking, BigDecimal oldSegmentTotal, BigDecimal newSegmentTotal, int nightsRepriced) {
    }

    private BookingSegmentEntity requireSegmentOnBooking(String bookingId, String segmentId) {
        BookingSegmentEntity segment = segmentRepository.findById(segmentId).orElseThrow(() -> new NotFoundException("Segment not found"));
        if (!segment.getBookingId().equals(bookingId)) {
            throw new NotFoundException("Segment not found");
        }
        return segment;
    }

    private static Optional<BookingSegmentEntity> findSegmentContaining(List<BookingSegmentEntity> segments, LocalDate date) {
        return segments.stream().filter(s -> !date.isBefore(s.getCheckIn()) && date.isBefore(s.getCheckOut())).findFirst();
    }

    private BookingSegmentEntity requireSoleSegment(String bookingId) {
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.size() != 1) {
            throw new ConflictException(MULTI_SEGMENT_MESSAGE);
        }
        return segments.get(0);
    }

    /**
     * The continuity invariant - segments cover {@code [booking.checkIn, booking.checkOut)}
     * with no gap and no overlap - re-checked after every write that touches segments, not just
     * at relocate time. A violation means something upstream (a bug, or a hand-edited row) broke
     * an assumption every other read in this codebase relies on, so this throws an unchecked
     * exception (500) rather than a user-facing error: there is no correct user-facing message
     * for "the data is already wrong."
     */
    private static void assertContinuity(BookingEntity booking, List<BookingSegmentEntity> segments) {
        if (segments.isEmpty()) {
            throw new IllegalStateException("Booking " + booking.getId() + " has no segments");
        }
        List<BookingSegmentEntity> sorted = segments.stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();
        BookingSegmentEntity first = sorted.get(0);
        BookingSegmentEntity last = sorted.get(sorted.size() - 1);
        if (!first.getCheckIn().equals(booking.getCheckIn())) {
            throw new IllegalStateException(
                    "Booking " + booking.getId() + ": first segment checkIn " + first.getCheckIn() + " != booking checkIn " + booking.getCheckIn());
        }
        if (!last.getCheckOut().equals(booking.getCheckOut())) {
            throw new IllegalStateException(
                    "Booking " + booking.getId() + ": last segment checkOut " + last.getCheckOut() + " != booking checkOut " + booking.getCheckOut());
        }
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDate end = sorted.get(i).getCheckOut();
            LocalDate nextStart = sorted.get(i + 1).getCheckIn();
            if (!end.equals(nextStart)) {
                throw new IllegalStateException(
                        "Booking " + booking.getId() + ": gap/overlap between segments at " + end + " vs " + nextStart);
            }
        }
    }

    /**
     * Keeps {@link BookingEntity}'s denormalized roomId/roomUnitId/checkIn/checkOut/totalPrice
     * in sync with its segments - see the class javadoc for why those columns still exist.
     * roomId/roomUnitId mirror the *last* (by checkOut) segment: "what room is this booking in"
     * means "what room does the guest end up in / are they in right now," the same way a real
     * front desk would answer that question.
     */
    private static void syncBookingFromSegments(BookingEntity booking, List<BookingSegmentEntity> segments) {
        List<BookingSegmentEntity> sorted = segments.stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();
        BookingSegmentEntity first = sorted.get(0);
        BookingSegmentEntity last = sorted.get(sorted.size() - 1);
        booking.setCheckIn(first.getCheckIn());
        booking.setCheckOut(last.getCheckOut());
        booking.setRoomId(last.getRoomId());
        booking.setRoomUnitId(last.getRoomUnitId());
        BigDecimal total = sorted.stream().map(BookingSegmentEntity::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        booking.setTotalPrice(total);
    }

    /** Result of {@link #quoteSchedule}/{@link #quoteRelocation} - mirrors what the matching apply call would do without writing. */
    public record ScheduleQuote(BigDecimal totalPrice, int nights, boolean available, String reason) {
    }

    private record UnitConflict(String message, boolean badRequest) {
    }

    private boolean isCurrentSegment(List<BookingSegmentEntity> segments, BookingSegmentEntity segment) {
        BookingSegmentEntity current = OverstayRule.currentSegment(segments, LocalDate.now(clock));
        return current != null && current.getId().equals(segment.getId());
    }

    /**
     * A guest in the house cannot be moved into a room another guest is checked into - check-in's own block, asked by
     * every write that can change where they are now (assignment, relocation and its undo, a schedule change, a swap),
     * and by their quotes. The availability checks beside it do not see it: someone due out today holds no night from
     * today on, and an overdue guest being re-roomed is checked only against nights already past.
     *
     * <p>Null when this changes nothing about who is where: the booking is not {@code CHECKED_IN}, {@code moved} is not
     * the segment the stay is in today, {@code target} is the room they are already in, or nobody else is there. A unit
     * of another type or an inactive one is left to {@link #checkUnitBasics}. {@code vacating} are the bookings leaving
     * their room in the same write (the two sides of a swap).
     */
    private UnitConflict occupantConflict(
            BookingEntity booking,
            List<BookingSegmentEntity> segments,
            BookingSegmentEntity moved,
            RoomUnitEntity target,
            String targetRoomId,
            Set<String> vacating) {
        if (!movesNow(booking, segments, moved, target, targetRoomId)) {
            return null;
        }
        Set<String> except = new HashSet<>(vacating);
        except.add(booking.getId());
        return OverstayRule.guestInExcept(overstayRule.inHouseByRoomUnit(), target.getId(), except)
                .map(other -> new UnitConflict(
                        OverstayRule.occupiedMessage(target.getLabel(), other, "Check them out first, or choose another room."), false))
                .orElse(null);
    }

    /** {@link #occupantConflict} for a write: the room's row is locked first, as check-in locks it, so the two can't both pass. */
    private UnitConflict lockedOccupantConflict(
            BookingEntity booking,
            List<BookingSegmentEntity> segments,
            BookingSegmentEntity moved,
            RoomUnitEntity target,
            String targetRoomId,
            Set<String> vacating) {
        if (!movesNow(booking, segments, moved, target, targetRoomId)) {
            return null;
        }
        roomUnitRepository.findByIdForUpdate(target.getId());
        return occupantConflict(booking, segments, moved, target, targetRoomId, vacating);
    }

    private boolean movesNow(
            BookingEntity booking, List<BookingSegmentEntity> segments, BookingSegmentEntity moved, RoomUnitEntity target, String targetRoomId) {
        return booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN
                && isCurrentSegment(segments, moved)
                && target.getRoomId().equals(targetRoomId)
                && target.isActive()
                && !target.getId().equals(OverstayRule.unitOn(booking, segments, LocalDate.now(clock), clock));
    }

    /**
     * A write that took a room off a checked-in guest's stay leaves them in an earlier one ({@link
     * OverstayRule#unitOn}) or is refused: with no room to fall back to they would be nowhere, and a room they fall
     * back to may meanwhile hold somebody else.
     */
    private void requireGuestStillHasARoom(BookingEntity booking, String livedIn, List<BookingSegmentEntity> segmentsAfter) {
        if (booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
            return;
        }
        String now = OverstayRule.unitOn(booking, segmentsAfter, LocalDate.now(clock), clock);
        if (now == null) {
            if (livedIn != null) {
                throw ValidationException.field("roomUnitId", GUEST_NEEDS_A_ROOM);
            }
            return;
        }
        if (now.equals(livedIn)) {
            return;
        }
        RoomUnitEntity back = roomUnitRepository.findByIdForUpdate(now).orElseThrow(() -> new NotFoundException("Room unit not found"));
        OverstayRule.guestInExcept(overstayRule.inHouseByRoomUnit(), now, Set.of(booking.getId())).ifPresent(other -> {
            throw new ConflictException(OverstayRule.occupiedMessage(
                    back.getLabel(), other, "Without a new room this guest would be back in it - choose another room instead."));
        });
    }

    /** The quote's side of {@link #requireGuestStillHasARoom}: the stay as the change would leave it, on copies that are never saved. */
    private boolean leavesGuestWithoutRoom(BookingEntity booking, List<BookingSegmentEntity> segments, ScheduleTarget target) {
        if (booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN) {
            return false;
        }
        LocalDate today = LocalDate.now(clock);
        if (OverstayRule.unitOn(booking, segments, today, clock) == null) {
            return false;
        }
        List<BookingSegmentEntity> after = segments.stream()
                .filter(s -> !target.dropped().contains(s))
                .map(s -> {
                    boolean changed = s.getId().equals(target.segment().getId());
                    BookingSegmentEntity copy = new BookingSegmentEntity();
                    copy.setRoomUnitId(changed ? null : s.getRoomUnitId());
                    copy.setCheckIn(changed ? target.segmentCheckIn() : s.getCheckIn());
                    copy.setCheckOut(changed ? target.segmentCheckOut() : s.getCheckOut());
                    return copy;
                })
                .toList();
        return OverstayRule.unitOn(booking, after, today, clock) == null;
    }

    private static void failIfUnassignable(UnitConflict conflict) {
        if (conflict == null) {
            return;
        }
        if (conflict.badRequest()) {
            throw new BadRequestException(conflict.message());
        }
        throw new ConflictException(conflict.message());
    }

    /**
     * The room-type/active/block checks shared by every way of putting a physical room under a
     * segment - {@link #checkUnitAssignable} (excludes one booking's own segments) and
     * {@link #checkUnitAssignableExcludingSegments} (excludes a named set of segments, for
     * {@link #swapSegmentRoomUnits}) both layer their own overlap query on top of this.
     */
    private UnitConflict checkUnitBasics(RoomUnitEntity unit, String roomId, LocalDate checkIn, LocalDate checkOut) {
        if (!unit.getRoomId().equals(roomId)) {
            return new UnitConflict("Room " + unit.getLabel() + " is a different room type than requested", true);
        }
        if (!unit.isActive()) {
            return new UnitConflict("Room " + unit.getLabel() + " is not active", true);
        }

        List<RoomUnitBlockEntity> blocks =
                roomUnitBlockRepository.findByRoomUnitIdAndFromDateLessThanAndToDateGreaterThanEqual(unit.getId(), checkOut, checkIn);
        if (!blocks.isEmpty()) {
            RoomUnitBlockEntity block = blocks.get(0);
            return new UnitConflict("Room " + unit.getLabel() + " is blocked (" + block.getReason() + ") from " + DateText.date(block.getFromDate())
                    + " to " + DateText.date(block.getToDate()), false);
        }
        return null;
    }

    /**
     * The three checks {@code PUT /bookings/{id}/room-unit}, {@code PUT /bookings/{id}/segments/
     * {segmentId}/room-unit}, {@code PATCH /bookings/{id}/schedule}, {@code POST /bookings/staff}
     * and {@code POST /bookings/{id}/relocate} all need before letting a physical room take a
     * segment: same room type, active, not blocked, not already booked by another segment for an
     * overlapping stay. {@code excludeBookingId} is the booking being (re)assigned so its own
     * other segment(s) never conflict with it - {@code null} only for a booking that doesn't
     * exist yet ({@link #insertStaff}).
     */
    private UnitConflict checkUnitAssignable(
            RoomUnitEntity unit, String roomId, LocalDate checkIn, LocalDate checkOut, String excludeBookingId) {
        UnitConflict basics = checkUnitBasics(unit, roomId, checkIn, checkOut);
        if (basics != null) {
            return basics;
        }

        List<BookingSegmentEntity> overlapping = excludeBookingId == null
                ? segmentRepository.findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn)
                : segmentRepository.findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndBookingIdNot(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn, excludeBookingId);
        if (!overlapping.isEmpty()) {
            return new UnitConflict("Room " + unit.getLabel() + " is already booked for an overlapping stay", false);
        }
        UnitConflict lived = stillLivedInConflict(
                unit, checkIn, checkOut, excludeBookingId == null ? Set.of() : Set.of(excludeBookingId));
        if (lived != null) {
            return lived;
        }
        return overstayConflict(unit, roomId, checkIn, checkOut, o -> !o.bookingId().equals(excludeBookingId));
    }

    /**
     * Tonight, in a room somebody has not left: their move took effect with no new room picked, so no segment holds
     * this night ({@link OverstayRule#stillLivingIn}). Unit-level only - the type's count already has their night,
     * and later nights are not held.
     */
    private UnitConflict stillLivedInConflict(RoomUnitEntity unit, LocalDate checkIn, LocalDate checkOut, Set<String> excludeBookingIds) {
        LocalDate today = LocalDate.now(clock);
        if (checkIn.isAfter(today) || !checkOut.isAfter(today)) {
            return null;
        }
        return overstayRule.stillLivingIn(unit.getId(), excludeBookingIds)
                .map(guest -> new UnitConflict(OverstayRule.occupiedMessage(unit.getLabel(), guest, ""), false))
                .orElse(null);
    }

    /**
     * A guest still checked in past checkOut holds their last segment's room through tonight
     * (see {@link OverstayRule}) - no {@code BookingSegment} row says so, so every unit-level
     * check asks here as well. Read inside the caller's SERIALIZABLE transaction like the
     * segment query beside it, so a concurrent check-out or extension is a serialization
     * conflict, not a missed one.
     */
    private UnitConflict overstayConflict(
            RoomUnitEntity unit, String roomId, LocalDate checkIn, LocalDate checkOut, java.util.function.Predicate<OverstayRule.Overstay> counts) {
        return overstayRule.overlapping(roomId, checkIn, checkOut).stream()
                .filter(o -> unit.getId().equals(o.roomUnitId()))
                .filter(counts)
                .findFirst()
                .map(o -> new UnitConflict("Room " + unit.getLabel() + " is still occupied by " + o.booking().getGuestName()
                        + ", who was due to check out on " + DateText.date(o.booking().getCheckOut()) + " and hasn't been checked out", false))
                .orElse(null);
    }

    /**
     * Same shape as {@link #checkUnitAssignable}, but excludes a named <em>set</em> of segment
     * ids rather than one booking's own - {@link #swapSegmentRoomUnits}'s own conflict check,
     * where only the two segments actually swapping should be excluded, not their whole parent
     * bookings (a relocated booking's other segments, or a third booking entirely, must still
     * conflict-check normally against the unit either side is moving into).
     */
    private UnitConflict checkUnitAssignableExcludingSegments(
            RoomUnitEntity unit, String roomId, LocalDate checkIn, LocalDate checkOut, Set<String> excludeSegmentIds) {
        UnitConflict basics = checkUnitBasics(unit, roomId, checkIn, checkOut);
        if (basics != null) {
            return basics;
        }

        List<BookingSegmentEntity> overlapping = segmentRepository
                .findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNotIn(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn, excludeSegmentIds);
        if (!overlapping.isEmpty()) {
            return new UnitConflict("Room " + unit.getLabel() + " is already booked for an overlapping stay", false);
        }
        Set<String> swapping = segmentRepository.findAllById(excludeSegmentIds).stream()
                .map(BookingSegmentEntity::getBookingId)
                .collect(Collectors.toSet());
        UnitConflict lived = stillLivedInConflict(unit, checkIn, checkOut, swapping);
        if (lived != null) {
            return lived;
        }
        return overstayConflict(unit, roomId, checkIn, checkOut, o -> !excludeSegmentIds.contains(o.lastSegment().getId()));
    }

    /**
     * available(date) = activeUnitCount - distinct blocked units(date) - segments covering
     * date. A date is covered by a segment if checkIn &lt;= date &lt; checkOut (checkout day is
     * free). The range is available only if every night in [checkIn, checkOut) has at least one
     * unit left - this, plus the SERIALIZABLE isolation this method runs under, is what stops
     * two concurrent requests from both taking the last unit on the same date.
     * {@code excludeBookingId}, when non-null, excludes that booking's own segments from the
     * overlap count - required so a booking extending/moving/relocating its own stay never
     * conflicts with itself.
     */
    private boolean isRangeAvailable(String roomId, int unitCount, LocalDate checkIn, LocalDate checkOut, String excludeBookingId) {
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);

        List<String> unitIds = roomUnitRepository.findByRoomIdAndIsActiveTrue(roomId).stream().map(RoomUnitEntity::getId).toList();
        List<RoomUnitBlockEntity> blocks = unitIds.isEmpty()
                ? List.of()
                : roomUnitBlockRepository.findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        unitIds, checkOut.minusDays(1), checkIn);

        List<BookingSegmentEntity> overlapping = excludeBookingId == null
                ? segmentRepository.findByRoomIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                        roomId, BookingStatus.CANCELLED, checkOut, checkIn)
                : segmentRepository.findByRoomIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndBookingIdNot(
                        roomId, BookingStatus.CANCELLED, checkOut, checkIn, excludeBookingId);
        List<OverstayRule.Overstay> overstays = overstayRule.overlapping(roomId, checkIn, checkOut).stream()
                .filter(o -> !o.bookingId().equals(excludeBookingId))
                .toList();

        for (LocalDate night : nights) {
            long blockedUnits = blocks.stream()
                    .filter(b -> !night.isBefore(b.getFromDate()) && !night.isAfter(b.getToDate()))
                    .map(RoomUnitBlockEntity::getRoomUnitId)
                    .distinct()
                    .count();
            long bookedUnits = overlapping.stream()
                    .filter(b -> !night.isBefore(b.getCheckIn()) && night.isBefore(b.getCheckOut()))
                    .count()
                    + overstays.stream().filter(o -> o.covers(night)).count();
            int available = InventoryMath.availableCount(unitCount, (int) blockedUnits, (int) bookedUnits);
            if (available < 1) {
                return false;
            }
        }
        return true;
    }

    /** Every night in {@code nights} priced at *today's* rate - RatePlan override where one exists, Room.basePrice otherwise. Never reads what a night was already priced at; see {@link #existingRatesByDate} for that. */
    private Map<LocalDate, BigDecimal> priceNightsAtCurrentRate(RoomEntity room, List<LocalDate> nights) {
        return roomNightPricing.priceNights(room, nights);
    }

    /** A segment's already-agreed nightly prices, keyed by date - what {@link #priceNightsAtCurrentRate} deliberately never looks at. */
    private Map<LocalDate, BigDecimal> existingRatesByDate(String segmentId) {
        return nightlyRateRepository.findBySegmentIdOrderByDateAsc(segmentId).stream()
                .collect(Collectors.toMap(BookingSegmentNightlyRateEntity::getDate, BookingSegmentNightlyRateEntity::getPrice));
    }

    private static BigDecimal sumPrices(Map<LocalDate, BigDecimal> prices) {
        return prices.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Persists one frozen-price row per entry - the only place a {@link BookingSegmentNightlyRateEntity} is ever created. */
    private void saveNightlyRates(String segmentId, Map<LocalDate, BigDecimal> prices) {
        for (Map.Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {
            BookingSegmentNightlyRateEntity rate = new BookingSegmentNightlyRateEntity();
            rate.setSegmentId(segmentId);
            rate.setDate(entry.getKey());
            rate.setPrice(entry.getValue());
            nightlyRateRepository.save(rate);
        }
        nightlyRateRepository.flush();
    }
}
