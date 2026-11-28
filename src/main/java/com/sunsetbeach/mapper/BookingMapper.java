package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.CheckInRoomNeeded;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PendingRoomMove;
import com.sunsetbeach.model.RoomStillHeld;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.service.BookingFolioCalculator;
import com.sunsetbeach.service.OverstayRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    private final RoomMapper roomMapper;
    private final RoomUnitMapper roomUnitMapper;
    private final GuestMapper guestMapper;
    private final BookingSegmentMapper segmentMapper;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BookingFolioCalculator folioCalculator;
    private final java.time.Clock clock;

    public BookingMapper(
            RoomMapper roomMapper,
            RoomUnitMapper roomUnitMapper,
            GuestMapper guestMapper,
            BookingSegmentMapper segmentMapper,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            BookingFolioCalculator folioCalculator,
            java.time.Clock clock) {
        this.folioCalculator = folioCalculator;
        this.clock = clock;
        this.roomMapper = roomMapper;
        this.roomUnitMapper = roomUnitMapper;
        this.guestMapper = guestMapper;
        this.segmentMapper = segmentMapper;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
    }

    /**
     * checkIn/checkOut render as plain date-only strings (see {@code entity.getCheckIn()
     * .toString()} below), the same convention as {@link BookingSegmentMapper} - {@code Booking}
     * used to render these as a datetime with a legacy {@code T00:00:00.000Z} artifact, unified
     * with everything else on 2026-08-30 (see {@code openapi.yaml}'s {@code Booking} schema
     * description for why). {@code roomUnit} is null until a physical room has been
     * assigned via {@code PUT /bookings/{id}/room-unit}. {@code guest} is null until reception
     * links one via {@code PUT /bookings/{id}/guest} - independent of {@code roomUnit}'s own
     * nullability, resolved by the same caller convention (see this class's own callers).
     * {@code segments} must be this booking's
     * full, ordered segment list (never empty) - callers get it from
     * {@code BookingSegmentRepository.findByBookingIdOrderByCheckInAsc}, the one place that
     * query lives. {@code room}/{@code roomUnit} passed in are the *last* segment's (matching
     * {@link BookingEntity}'s own denormalized roomId/roomUnitId - see {@code BookingWriter}'s
     * javadoc), not re-derived here.
     *
     * <p>Each segment's own room/unit is resolved by an explicit bulk
     * {@code findAllById} here, not via {@link BookingSegmentEntity#getRoom()}'s lazy
     * association: segments are frequently loaded in a short-lived repository-only call (most
     * `BookingService` write methods aren't themselves `@Transactional` - the SERIALIZABLE
     * transaction lives in `BookingWriter`, already committed by the time this mapper runs), so
     * a lazy nav here would throw `LazyInitializationException` outside that closed session.
     */
    /** Where the booking stands as a stay - see {@code StayStatus} in openapi.yaml. */
    static com.sunsetbeach.model.StayStatus stayStatusOf(BookingEntity entity) {
        if (entity.getStatus() == com.sunsetbeach.model.BookingStatus.CANCELLED) {
            return com.sunsetbeach.model.StayStatus.CANCELLED;
        }
        return switch (entity.getOccupancyStatus()) {
            case NO_SHOW -> com.sunsetbeach.model.StayStatus.NO_SHOW;
            case CHECKED_OUT -> com.sunsetbeach.model.StayStatus.CHECKED_OUT;
            case CHECKED_IN -> com.sunsetbeach.model.StayStatus.IN_HOUSE;
            case EXPECTED -> com.sunsetbeach.model.StayStatus.UPCOMING;
        };
    }

    /**
     * Why this booking can't be checked in any more, or empty: its stay is over ({@code today} is past
     * {@code checkOut}) and the guest never arrived. The one rule and the one sentence, for {@code
     * BookingOccupancyService#checkIn}'s 409 and for every screen that would otherwise offer the button. On the
     * departure day check-in is still allowed - a guest arriving after midnight for last night.
     */
    public static Optional<String> checkInClosedReason(BookingEntity entity, LocalDate today) {
        boolean awaiting = entity.getStatus() != com.sunsetbeach.model.BookingStatus.CANCELLED
                && (entity.getOccupancyStatus() == OccupancyStatus.EXPECTED || entity.getOccupancyStatus() == OccupancyStatus.NO_SHOW);
        return awaiting && today.isAfter(entity.getCheckOut())
                ? Optional.of("This stay ended on " + DateText.date(entity.getCheckOut()) + ". Change the dates first, then check in.")
                : Optional.empty();
    }

    /**
     * The part of the stay that must get a room before this booking can be checked in today, or empty - nothing to
     * ask for: not awaiting a check-in that is possible today, or tonight's room is there. What {@code
     * BookingOccupancyService#checkIn} refuses, said in advance so a screen can offer the room picker for exactly that
     * segment. {@code roomName}: a room type's name by its id.
     */
    public static Optional<CheckInRoomNeeded> checkInRoomNeeded(
            BookingEntity entity, List<BookingSegmentEntity> segments, Function<String, String> roomName, LocalDate today) {
        boolean awaiting = entity.getStatus() != com.sunsetbeach.model.BookingStatus.CANCELLED
                && (entity.getOccupancyStatus() == OccupancyStatus.EXPECTED || entity.getOccupancyStatus() == OccupancyStatus.NO_SHOW);
        if (!awaiting || today.isBefore(entity.getCheckIn()) || today.isAfter(entity.getCheckOut())) {
            return Optional.empty();
        }
        BookingSegmentEntity current = OverstayRule.currentSegment(segments, today);
        if (current == null || current.getRoomUnitId() != null) {
            return Optional.empty();
        }
        return Optional.of(new CheckInRoomNeeded(
                current.getId(),
                current.getRoomId(),
                roomName.apply(current.getRoomId()),
                nightCheckedInto(entity, today).toString(),
                current.getCheckIn().toString(),
                current.getCheckOut().toString(),
                roomNeededReason(entity, segments, today)));
    }

    /** {@code POST /bookings/{id}/check-in}'s 400 when tonight has no room: a moved stay names the night. */
    public static String roomNeededReason(BookingEntity entity, List<BookingSegmentEntity> segments, LocalDate today) {
        return segments.size() > 1
                ? "Assign a room for the night of " + DateText.date(nightCheckedInto(entity, today)) + " before checking in."
                : "Assign a room before checking in.";
    }

    /** The night a check-in made today is for: tonight, or on the departure day the one just ending. */
    private static LocalDate nightCheckedInto(BookingEntity entity, LocalDate today) {
        return today.isBefore(entity.getCheckOut()) ? today : entity.getCheckOut().minusDays(1);
    }

    /** The warning on the guest moved into a room whose waiting guest has not left it ({@link OverstayRule#roomStillHeld}). */
    public static RoomStillHeld roomStillHeld(OverstayRule.DoubledRoom doubled, String unitLabel) {
        return new RoomStillHeld(
                doubled.holder().booking().getId(),
                doubled.holder().booking().getGuestName(),
                OverstayRule.stillHeldMessage(unitLabel, doubled.holder()));
    }

    /**
     * Present for a guest in the house whose part of the stay holding today has no room: their move has taken effect
     * (or the room was taken off) and nobody picked the new one. They are still where they were ({@link
     * OverstayRule#unitOn}); the screens show {@code reason} with a way to pick the room for exactly that part.
     */
    public static Optional<PendingRoomMove> pendingRoomMove(
            BookingEntity entity,
            List<BookingSegmentEntity> segments,
            Function<String, String> roomName,
            Function<String, String> unitLabel,
            java.time.Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (!OverstayRule.awaitsRoom(entity, segments, today)) {
            return Optional.empty();
        }
        BookingSegmentEntity current = OverstayRule.currentSegment(segments, today);
        BookingSegmentEntity livedIn = OverstayRule.roomSegmentOn(entity, segments, today, clock);
        String stayingIn = livedIn.getRoomUnitId() == null ? null : unitLabel.apply(livedIn.getRoomUnitId());
        String reason = current.getCheckIn().equals(today)
                ? "Moving today — choose the new room"
                : stayingIn != null
                        ? "Move of " + DateText.date(current.getCheckIn()) + " not completed — choose the new room"
                        : "No room assigned — choose one";
        PendingRoomMove pending = new PendingRoomMove(
                current.getId(),
                current.getRoomId(),
                roomName.apply(current.getRoomId()),
                current.getCheckIn().toString(),
                current.getCheckOut().toString(),
                reason);
        return Optional.of(pending.stayingInRoom(stayingIn));
    }

    public Booking toDto(BookingEntity entity, RoomEntity room, RoomUnitEntity roomUnit, GuestEntity guest, List<BookingSegmentEntity> segments) {
        return toDto(entity, room, roomUnit, guest, segments, folioCalculator.breakdown(entity));
    }

    /**
     * {@code balanceDue}: what the guest owes right now ({@link BookingFolioCalculator}). A list
     * passes the figure it computed for the whole page in two queries ({@link
     * BookingFolioCalculator#breakdowns}) instead of letting each row ask for its own.
     */
    public Booking toDto(
            BookingEntity entity,
            RoomEntity room,
            RoomUnitEntity roomUnit,
            GuestEntity guest,
            List<BookingSegmentEntity> segments,
            BookingFolioCalculator.FolioBreakdown folio) {
        BigDecimal balanceDue = folio.balanceDue();
        List<BookingSegmentEntity> sorted = segments.stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();
        BookingSegmentEntity current = OverstayRule.currentSegment(sorted, java.time.LocalDate.now(clock));

        Map<String, RoomEntity> roomsById = roomRepository
                .findAllById(sorted.stream().map(BookingSegmentEntity::getRoomId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(RoomEntity::getId, Function.identity()));
        List<String> roomUnitIds = sorted.stream().map(BookingSegmentEntity::getRoomUnitId).filter(Objects::nonNull).distinct().toList();
        Map<String, RoomUnitEntity> roomUnitsById = roomUnitIds.isEmpty()
                ? Map.of()
                : roomUnitRepository.findAllById(roomUnitIds).stream().collect(Collectors.toMap(RoomUnitEntity::getId, Function.identity()));

        return new Booking(
                entity.getId(),
                entity.getRoomId(),
                roomMapper.toDto(room),
                entity.getRoomUnitId(),
                roomUnit != null ? roomUnitMapper.toDto(roomUnit) : null,
                entity.getGuestName(),
                entity.getGuestEmail(),
                entity.getGuestPhone(),
                entity.getGuestId(),
                guest != null ? guestMapper.toDto(guest) : null,
                entity.getCheckIn().toString(),
                entity.getCheckOut().toString(),
                PriceFormat.asDecimalString(entity.getTotalPrice()),
                entity.getStatus(),
                entity.getChannel(),
                entity.getPurpose(),
                entity.getAdults(),
                entity.getChildren(),
                entity.getPaymentNote(),
                entity.getOccupancyStatus(),
                entity.getCheckedInAt() != null ? TimestampFormat.toUtc(entity.getCheckedInAt()) : null,
                entity.getCheckedOutAt() != null ? TimestampFormat.toUtc(entity.getCheckedOutAt()) : null,
                entity.getExternalReference(),
                entity.getExternalChannel(),
                entity.getCancellationReason(),
                sorted.stream()
                        .map(s -> segmentMapper.toDto(
                                s, roomsById.get(s.getRoomId()), s.getRoomUnitId() != null ? roomUnitsById.get(s.getRoomUnitId()) : null))
                        .toList(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()))
                .stayStatus(stayStatusOf(entity))
                .currentSegmentId(current != null ? current.getId() : null)
                .roomSegmentId(current != null ? OverstayRule.roomSegmentOn(entity, sorted, java.time.LocalDate.now(clock), clock).getId() : null)
                .pendingRoomMove(pendingRoomMove(
                                entity, sorted, id -> roomsById.get(id).getName(), id -> roomUnitsById.get(id).getLabel(), clock)
                        .orElse(null))
                .checkInClosedReason(checkInClosedReason(entity, java.time.LocalDate.now(clock)).orElse(null))
                .checkInRoomNeeded(checkInRoomNeeded(entity, sorted, id -> roomsById.get(id).getName(), java.time.LocalDate.now(clock)).orElse(null))
                .notes(entity.getNotes())
                .breakfastGuests(entity.getBreakfastGuests())
                .breakfastRate(entity.getBreakfastRate() != null ? com.sunsetbeach.model.BreakfastRate.fromValue(entity.getBreakfastRate()) : null)
                .bookedAt(entity.getBookedAt() != null ? TimestampFormat.toUtc(entity.getBookedAt()) : null)
                .earlyDepartureFee(PriceFormat.asDecimalString(entity.getEarlyDepartureFee()))
                .balanceDue(PriceFormat.asDecimalString(balanceDue))
                .settlement(folio.settlement())
                .creditBalance(PriceFormat.asDecimalString(folio.creditBalance()))
                .owes(BookingFolioCalculator.debtHasFallenDue(entity, balanceDue, java.time.LocalDate.now(clock)));
    }
}
