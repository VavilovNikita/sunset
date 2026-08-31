package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    private final RoomMapper roomMapper;
    private final RoomUnitMapper roomUnitMapper;
    private final BookingSegmentMapper segmentMapper;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;

    public BookingMapper(
            RoomMapper roomMapper,
            RoomUnitMapper roomUnitMapper,
            BookingSegmentMapper segmentMapper,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository) {
        this.roomMapper = roomMapper;
        this.roomUnitMapper = roomUnitMapper;
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
     * assigned via {@code PUT /bookings/{id}/room-unit}. {@code segments} must be this booking's
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
    public Booking toDto(BookingEntity entity, RoomEntity room, RoomUnitEntity roomUnit, List<BookingSegmentEntity> segments) {
        List<BookingSegmentEntity> sorted = segments.stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();

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
                entity.getCheckIn().toString(),
                entity.getCheckOut().toString(),
                PriceFormat.asDecimalString(entity.getTotalPrice()),
                entity.getStatus(),
                entity.getPaymentNote(),
                sorted.stream()
                        .map(s -> segmentMapper.toDto(
                                s, roomsById.get(s.getRoomId()), s.getRoomUnitId() != null ? roomUnitsById.get(s.getRoomUnitId()) : null))
                        .toList(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()));
    }
}
