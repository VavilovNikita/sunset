package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    private final RoomMapper roomMapper;
    private final RoomUnitMapper roomUnitMapper;

    public BookingMapper(RoomMapper roomMapper, RoomUnitMapper roomUnitMapper) {
        this.roomMapper = roomMapper;
        this.roomUnitMapper = roomUnitMapper;
    }

    /**
     * checkIn/checkOut are date-only columns but the JSON contract renders them as
     * datetimes with a T00:00:00.000Z time component, matching how Prisma serializes a
     * @db.Date column's JS Date object. {@code roomUnit} is null until a physical room has been
     * assigned via {@code PUT /bookings/{id}/room-unit}.
     */
    public Booking toDto(BookingEntity entity, RoomEntity room, RoomUnitEntity roomUnit) {
        return new Booking(
                entity.getId(),
                entity.getRoomId(),
                roomMapper.toDto(room),
                entity.getRoomUnitId(),
                roomUnit != null ? roomUnitMapper.toDto(roomUnit) : null,
                entity.getGuestName(),
                entity.getGuestEmail(),
                entity.getGuestPhone(),
                entity.getCheckIn().atTime(LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC),
                entity.getCheckOut().atTime(LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC),
                PriceFormat.asDecimalString(entity.getTotalPrice()),
                entity.getStatus(),
                entity.getPaymentNote(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()));
    }
}
