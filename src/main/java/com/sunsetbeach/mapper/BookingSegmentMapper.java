package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.BookingSegment;
import org.springframework.stereotype.Component;

@Component
public class BookingSegmentMapper {

    private final RoomMapper roomMapper;
    private final RoomUnitMapper roomUnitMapper;

    public BookingSegmentMapper(RoomMapper roomMapper, RoomUnitMapper roomUnitMapper) {
        this.roomMapper = roomMapper;
        this.roomUnitMapper = roomUnitMapper;
    }

    /** checkIn/checkOut render as plain date-only strings, same convention as {@link BookingMapper#toDto}. */
    public BookingSegment toDto(BookingSegmentEntity entity, RoomEntity room, RoomUnitEntity roomUnit) {
        return new BookingSegment(
                entity.getId(),
                entity.getRoomId(),
                roomMapper.toDto(room),
                entity.getRoomUnitId(),
                roomUnit != null ? roomUnitMapper.toDto(roomUnit) : null,
                entity.getCheckIn().toString(),
                entity.getCheckOut().toString(),
                PriceFormat.asDecimalString(entity.getTotalPrice()));
    }
}
