package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomUnitBlockRepository extends JpaRepository<RoomUnitBlockEntity, String> {

    List<RoomUnitBlockEntity> findByRoomUnitId(String roomUnitId);

    Optional<RoomUnitBlockEntity> findByIdAndRoomUnitId(String id, String roomUnitId);

    /** Blocks of the given units overlapping [rangeStart, rangeEnd] (both inclusive) - used to render a month's inventory. */
    List<RoomUnitBlockEntity> findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            List<String> roomUnitIds, LocalDate rangeEnd, LocalDate rangeStart);

    /**
     * Blocks of one unit overlapping a booking's half-open stay [checkIn, checkOut) - used by
     * the room-assignment race check in {@link com.sunsetbeach.service.BookingWriter}.
     */
    List<RoomUnitBlockEntity> findByRoomUnitIdAndFromDateLessThanAndToDateGreaterThanEqual(
            String roomUnitId, LocalDate checkOut, LocalDate checkIn);
}
