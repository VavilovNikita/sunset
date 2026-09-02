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
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p><b>{@code assignRoomUnit}/{@code unassignRoomUnit} only operate on a booking with exactly
 * one segment.</b> Once a booking has been relocated, "the room" is no longer a single
 * well-defined value a bare {@code roomUnitId} can express - which segment would it apply to?
 * Rather than guess, these calls reject with a clear message once segments.size() > 1, and
 * direct the caller to {@link #relocate}/{@link #undoRelocation}, the two operations actually
 * built to reason about more than one segment at a time.
 *
 * <p><b>{@code updateSchedule}/{@code quoteSchedule} allow a narrower case on a multi-segment
 * booking:</b> a change that moves only the outer edge of the first or last segment (the
 * booking's own overall checkIn or checkOut, respectively) without crossing into the
 * neighboring segment. That covers the two most common front-desk requests on an
 * already-relocated stay - "one more night" (extend the last segment, same room it's already
 * in) and an earlier arrival (extend the first segment) - without forcing the roundabout
 * undo-relocate / update / re-relocate dance. Anything else against a multi-segment booking
 * (both ends moving at once, a date range that would cross a segment boundary, or a bare room
 * change with no date change - none of which map to a single unambiguous segment given this
 * endpoint's one roomUnitId/checkIn/checkOut shape) is rejected the same way, in favor of
 * {@link #relocate}/{@link #undoRelocation}. See {@link #resolveScheduleTarget}.
 */
@Service
public class BookingWriter {

    static final String MULTI_SEGMENT_MESSAGE =
            "This booking has been split by a room relocation — change dates or rooms per segment via relocate/undo-relocate instead.";

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final BookingSegmentNightlyRateRepository nightlyRateRepository;
    private final RatePlanRepository ratePlanRepository;

    public BookingWriter(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            BookingSegmentNightlyRateRepository nightlyRateRepository,
            RatePlanRepository ratePlanRepository) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.nightlyRateRepository = nightlyRateRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insert(
            RoomEntity room, String guestName, String guestEmail, String guestPhone, LocalDate checkIn, LocalDate checkOut) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        Map<LocalDate, BigDecimal> nightlyPrices = priceNightsAtCurrentRate(room, DateRangeUtil.getNights(checkIn, checkOut));
        BookingEntity entity =
                newBookingEntity(room, guestName, guestEmail, guestPhone, checkIn, checkOut, BookingSource.PUBLIC, sumPrices(nightlyPrices));
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
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insertStaff(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            String roomUnitId) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        Map<LocalDate, BigDecimal> nightlyPrices = priceNightsAtCurrentRate(room, DateRangeUtil.getNights(checkIn, checkOut));
        BookingEntity entity =
                newBookingEntity(room, guestName, guestEmail, guestPhone, checkIn, checkOut, BookingSource.STAFF, sumPrices(nightlyPrices));

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

    private BookingEntity newBookingEntity(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingSource source,
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

        failIfUnassignable(checkUnitAssignable(unit, segment.getRoomId(), segment.getCheckIn(), segment.getCheckOut(), bookingId));

        segment.setRoomUnitId(unit.getId());
        segmentRepository.saveAndFlush(segment);
        syncBookingFromSegments(booking, List.of(segment));
        return bookingRepository.saveAndFlush(booking);
    }

    /** No contention to protect against when clearing an assignment, so plain default isolation is enough. */
    @Transactional
    public BookingEntity unassignRoomUnit(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingSegmentEntity segment = requireSoleSegment(bookingId);
        segment.setRoomUnitId(null);
        segmentRepository.saveAndFlush(segment);
        syncBookingFromSegments(booking, List.of(segment));
        return bookingRepository.saveAndFlush(booking);
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
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.isEmpty()) {
            throw new NotFoundException("Booking not found");
        }
        ScheduleTarget target = resolveScheduleTarget(segments, newCheckIn, newCheckOut);
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

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, segCheckIn, segCheckOut, bookingId)) {
            throw new ConflictException("Selected dates are no longer available");
        }

        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
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
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.isEmpty()) {
            throw new NotFoundException("Booking not found");
        }
        ScheduleTarget target = resolveScheduleTarget(segments, newCheckIn, newCheckOut);
        if (target.isRejected()) {
            return new ScheduleQuote(BigDecimal.ZERO, 0, false, target.rejectionReason());
        }
        BookingSegmentEntity segment = target.segment();
        LocalDate segCheckIn = target.segmentCheckIn();
        LocalDate segCheckOut = target.segmentCheckOut();
        RoomEntity room = roomRepository.findById(segment.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        ScheduleReprice reprice = repriceSchedule(segment, room, segCheckIn, segCheckOut);
        BigDecimal othersTotal = segments.stream()
                .filter(s -> !s.getId().equals(segment.getId()))
                .map(BookingSegmentEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice = othersTotal.add(reprice.newSegmentTotal());
        int nights = DateRangeUtil.getNights(newCheckIn, newCheckOut).size();

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, segCheckIn, segCheckOut, bookingId)) {
            return new ScheduleQuote(totalPrice, nights, false, "Selected dates are no longer available");
        }

        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            UnitConflict conflict = checkUnitAssignable(unit, room.getId(), segCheckIn, segCheckOut, bookingId);
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
     *       single segment this endpoint's shape can name, so it's rejected in favor of
     *       {@link #relocate}/{@link #undoRelocation}.</li>
     * </ul>
     */
    private ScheduleTarget resolveScheduleTarget(List<BookingSegmentEntity> sortedSegments, LocalDate newCheckIn, LocalDate newCheckOut) {
        if (sortedSegments.size() == 1) {
            return ScheduleTarget.of(sortedSegments.get(0), newCheckIn, newCheckOut);
        }
        BookingSegmentEntity first = sortedSegments.get(0);
        BookingSegmentEntity last = sortedSegments.get(sortedSegments.size() - 1);
        boolean checkInChanged = !newCheckIn.equals(first.getCheckIn());
        boolean checkOutChanged = !newCheckOut.equals(last.getCheckOut());

        if (checkInChanged && !checkOutChanged) {
            if (!newCheckIn.isBefore(first.getCheckOut())) {
                return ScheduleTarget.badRequest("New check-in must stay before " + first.getCheckOut()
                        + " - crossing into the next segment isn't a schedule change, it's a relocation.");
            }
            return ScheduleTarget.of(first, newCheckIn, first.getCheckOut());
        }
        if (checkOutChanged && !checkInChanged) {
            if (!newCheckOut.isAfter(last.getCheckIn())) {
                return ScheduleTarget.badRequest("New check-out must stay after " + last.getCheckIn()
                        + " - crossing into the previous segment isn't a schedule change, it's an undo-relocation.");
            }
            return ScheduleTarget.of(last, last.getCheckIn(), newCheckOut);
        }
        return ScheduleTarget.rejected(MULTI_SEGMENT_MESSAGE);
    }

    /** Result of {@link #resolveScheduleTarget} - either a segment to change and its new bounds, or a reason it can't be resolved. */
    private record ScheduleTarget(
            BookingSegmentEntity segment, LocalDate segmentCheckIn, LocalDate segmentCheckOut, String rejectionReason, boolean badRequest) {
        static ScheduleTarget of(BookingSegmentEntity segment, LocalDate checkIn, LocalDate checkOut) {
            return new ScheduleTarget(segment, checkIn, checkOut, null, false);
        }

        static ScheduleTarget rejected(String reason) {
            return new ScheduleTarget(null, null, null, reason, false);
        }

        static ScheduleTarget badRequest(String reason) {
            return new ScheduleTarget(null, null, null, reason, true);
        }

        boolean isRejected() {
            return segment == null;
        }
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
     * Splits the segment covering {@code effectiveDate} into two: the existing room/unit keeps
     * {@code [oldCheckIn, effectiveDate)} at whatever it was already priced, and a new segment
     * for {@code [effectiveDate, oldCheckOut)} takes {@code newRoomId}/{@code newRoomUnitId},
     * priced fresh at today's rate (a genuinely new agreement - a guest moving to a different
     * room, possibly a different room *type*, is exactly the case where a price change is
     * warranted, but only for the nights after the move). The truncated segment's own rows for
     * the nights it's giving up are deliberately left in place, not deleted, even though they now
     * fall outside its {@code [checkIn, checkOut)} range - see {@link #undoRelocation}'s comment
     * for why keeping them is what makes undoing this relocation later a real undo. Same three
     * checks as {@link #assignRoomUnit} against the *new* room/unit for the new segment's date
     * range, same SERIALIZABLE race-safety as {@link #insert}.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RelocationResult relocate(String bookingId, LocalDate effectiveDate, String newRoomId, String newRoomUnitId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        BookingSegmentEntity target = findSegmentContaining(segments, effectiveDate)
                .orElseThrow(() -> new BadRequestException("effectiveDate must fall within this booking's stay"));
        if (!effectiveDate.isAfter(target.getCheckIn())) {
            throw new BadRequestException(
                    "effectiveDate must be strictly after " + target.getCheckIn() + " - relocating on a segment's own first night just "
                            + "means assigning that segment a different room, not a mid-stay move");
        }

        RoomEntity oldRoom = roomRepository.findById(target.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomEntity newRoom = roomRepository.findById(newRoomId).orElseThrow(() -> new NotFoundException("Room not found"));
        LocalDate newSegCheckIn = effectiveDate;
        LocalDate newSegCheckOut = target.getCheckOut();

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(newRoom.getId());
        if (!isRangeAvailable(newRoom.getId(), unitCount, newSegCheckIn, newSegCheckOut, bookingId)) {
            throw new ConflictException("Selected room type has no availability for that period");
        }
        RoomUnitEntity newUnit = null;
        if (newRoomUnitId != null) {
            newUnit = roomUnitRepository.findById(newRoomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            failIfUnassignable(checkUnitAssignable(newUnit, newRoom.getId(), newSegCheckIn, newSegCheckOut, bookingId));
        }

        String oldUnitLabel = target.getRoomUnitId() != null
                ? roomUnitRepository.findById(target.getRoomUnitId()).map(RoomUnitEntity::getLabel).orElse(null)
                : null;

        List<LocalDate> movedOutNights = DateRangeUtil.getNights(effectiveDate, target.getCheckOut());
        BigDecimal movedOutSum =
                movedOutNights.stream().map(existingRatesByDate(target.getId())::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        target.setCheckOut(effectiveDate);
        target.setTotalPrice(target.getTotalPrice().subtract(movedOutSum));
        segmentRepository.saveAndFlush(target);
        // No delete here - see the class/method javadoc. These rows are the guest's original
        // price for these specific nights; keeping them around (inert, since they're now outside
        // target's own checkIn/checkOut) is what undoRelocation restores from.

        saveSegment(bookingId, newRoom.getId(), newUnit != null ? newUnit.getId() : null, newSegCheckIn, newSegCheckOut,
                priceNightsAtCurrentRate(newRoom, DateRangeUtil.getNights(newSegCheckIn, newSegCheckOut)));

        List<BookingSegmentEntity> updated = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        assertContinuity(booking, updated);
        syncBookingFromSegments(booking, updated);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        return new RelocationResult(saved, oldRoom, oldUnitLabel, newRoom, newUnit);
    }

    /** {@link #relocate}'s saved booking plus the old/new room identity, for the audit summary ("from X to Y"). */
    public record RelocationResult(BookingEntity booking, RoomEntity oldRoom, String oldUnitLabel, RoomEntity newRoom, RoomUnitEntity newUnit) {
    }

    /**
     * Read-only preview for {@code POST /bookings/{id}/relocate/quote} - same checks as
     * {@link #relocate}, reusing {@link ScheduleQuote} (nights/totalPrice describe the *new*
     * segment being created; {@code totalPrice} is folded into the recomputed whole-booking
     * total the same way {@link #quoteSchedule} reports one) rather than a second, near-identical
     * response shape.
     */
    @Transactional(readOnly = true)
    public ScheduleQuote quoteRelocation(String bookingId, LocalDate effectiveDate, String newRoomId, String newRoomUnitId) {
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        if (segments.isEmpty()) {
            throw new NotFoundException("Booking not found");
        }
        Optional<BookingSegmentEntity> maybeTarget = findSegmentContaining(segments, effectiveDate);
        if (maybeTarget.isEmpty() || !effectiveDate.isAfter(maybeTarget.get().getCheckIn())) {
            return new ScheduleQuote(BigDecimal.ZERO, 0, false,
                    "effectiveDate must fall strictly inside an existing segment, after its first night");
        }
        BookingSegmentEntity target = maybeTarget.get();
        RoomEntity newRoom = roomRepository.findById(newRoomId).orElseThrow(() -> new NotFoundException("Room not found"));
        LocalDate newSegCheckIn = effectiveDate;
        LocalDate newSegCheckOut = target.getCheckOut();
        int nights = DateRangeUtil.getNights(newSegCheckIn, newSegCheckOut).size();

        List<LocalDate> movedOutNights = DateRangeUtil.getNights(effectiveDate, target.getCheckOut());
        BigDecimal movedOutSum =
                movedOutNights.stream().map(existingRatesByDate(target.getId())::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shrunkOldSegmentPrice = target.getTotalPrice().subtract(movedOutSum);
        BigDecimal newSegmentPrice = sumPrices(priceNightsAtCurrentRate(newRoom, DateRangeUtil.getNights(newSegCheckIn, newSegCheckOut)));
        BigDecimal othersTotal = segments.stream()
                .filter(s -> !s.getId().equals(target.getId()))
                .map(BookingSegmentEntity::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal wholeBookingNewTotal = othersTotal.add(shrunkOldSegmentPrice).add(newSegmentPrice);

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(newRoom.getId());
        if (!isRangeAvailable(newRoom.getId(), unitCount, newSegCheckIn, newSegCheckOut, bookingId)) {
            return new ScheduleQuote(wholeBookingNewTotal, nights, false, "Selected room type has no availability for that period");
        }
        if (newRoomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(newRoomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            UnitConflict conflict = checkUnitAssignable(unit, newRoom.getId(), newSegCheckIn, newSegCheckOut, bookingId);
            if (conflict != null) {
                return new ScheduleQuote(wholeBookingNewTotal, nights, false, conflict.message());
            }
        }
        return new ScheduleQuote(wholeBookingNewTotal, nights, true, null);
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
                .orElseThrow(() -> new BadRequestException("No relocation boundary at " + splitDate));
        BookingSegmentEntity after = segments.stream().filter(s -> s.getCheckIn().equals(splitDate)).findFirst()
                .orElseThrow(() -> new BadRequestException("No relocation boundary at " + splitDate));

        RoomEntity room = roomRepository.findById(before.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomEntity discardedRoom = roomRepository.findById(after.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        String discardedUnitLabel = after.getRoomUnitId() != null
                ? roomUnitRepository.findById(after.getRoomUnitId()).map(RoomUnitEntity::getLabel).orElse(null)
                : null;
        LocalDate mergedCheckOut = after.getCheckOut();

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, splitDate, mergedCheckOut, bookingId)) {
            throw new ConflictException("Cannot undo — the earlier room type has no availability for the merged period");
        }
        if (before.getRoomUnitId() != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(before.getRoomUnitId()).orElseThrow(() -> new NotFoundException("Room unit not found"));
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
        return new RelocationResult(saved, discardedRoom, discardedUnitLabel, room,
                before.getRoomUnitId() != null ? roomUnitRepository.findById(before.getRoomUnitId()).orElse(null) : null);
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
        LocalDate today = LocalDate.now();
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
     * The three checks {@code PUT /bookings/{id}/room-unit}, {@code PATCH /bookings/{id}/schedule},
     * {@code POST /bookings/staff} and {@code POST /bookings/{id}/relocate} all need before
     * letting a physical room take a segment: same room type, active, not blocked, not already
     * booked by another segment for an overlapping stay. {@code excludeBookingId} is the booking
     * being (re)assigned so its own other segment(s) never conflict with it - {@code null} only
     * for a booking that doesn't exist yet ({@link #insertStaff}).
     */
    private UnitConflict checkUnitAssignable(
            RoomUnitEntity unit, String roomId, LocalDate checkIn, LocalDate checkOut, String excludeBookingId) {
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
            return new UnitConflict("Room " + unit.getLabel() + " is blocked (" + block.getReason() + ") from " + block.getFromDate()
                    + " to " + block.getToDate(), false);
        }

        List<BookingSegmentEntity> overlapping = excludeBookingId == null
                ? segmentRepository.findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn)
                : segmentRepository.findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndBookingIdNot(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn, excludeBookingId);
        if (!overlapping.isEmpty()) {
            return new UnitConflict("Room " + unit.getLabel() + " is already booked for an overlapping stay", false);
        }
        return null;
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

        for (LocalDate night : nights) {
            long blockedUnits = blocks.stream()
                    .filter(b -> !night.isBefore(b.getFromDate()) && !night.isAfter(b.getToDate()))
                    .map(RoomUnitBlockEntity::getRoomUnitId)
                    .distinct()
                    .count();
            long bookedUnits = overlapping.stream()
                    .filter(b -> !night.isBefore(b.getCheckIn()) && night.isBefore(b.getCheckOut()))
                    .count();
            int available = InventoryMath.availableCount(unitCount, (int) blockedUnits, (int) bookedUnits);
            if (available < 1) {
                return false;
            }
        }
        return true;
    }

    /** Every night in {@code nights} priced at *today's* rate - RatePlan override where one exists, Room.basePrice otherwise. Never reads what a night was already priced at; see {@link #existingRatesByDate} for that. */
    private Map<LocalDate, BigDecimal> priceNightsAtCurrentRate(RoomEntity room, List<LocalDate> nights) {
        if (nights.isEmpty()) {
            return Map.of();
        }
        Map<LocalDate, BigDecimal> overrides = new HashMap<>();
        for (RatePlanEntity plan : ratePlanRepository.findByRoomIdAndDateIn(room.getId(), nights)) {
            overrides.put(plan.getDate(), plan.getPrice());
        }
        Map<LocalDate, BigDecimal> prices = new LinkedHashMap<>();
        for (LocalDate night : nights) {
            prices.put(night, overrides.getOrDefault(night, room.getBasePrice()));
        }
        return prices;
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
