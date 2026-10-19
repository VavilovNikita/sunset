package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomUnitBlockRepository extends JpaRepository<RoomUnitBlockEntity, String> {

    List<RoomUnitBlockEntity> findByRoomUnitId(String roomUnitId);

    /** Blocks a job function answers for that are on at some point of [rangeStart, rangeEnd] (both inclusive) - the housekeeper's bot screen. */
    List<RoomUnitBlockEntity> findByResponsibleFunctionAndFromDateLessThanEqualAndToDateGreaterThanEqualOrderByFromDateAsc(
            String responsibleFunction, LocalDate rangeEnd, LocalDate rangeStart);

    /** Blocks whose last day is {@code toDate} - the morning "last day" reminder. */
    List<RoomUnitBlockEntity> findByToDate(LocalDate toDate);

    /** Blocks that ended before {@code today} while the maintenance task linked to them is not {@code done} - the daily "close it" reminder. */
    @Query("select b from RoomUnitBlockEntity b where b.toDate < :today and exists "
            + "(select 1 from MaintenanceTaskEntity t where t.blockId = b.id and t.status <> :done)")
    List<RoomUnitBlockEntity> findEndedWithOpenTask(
            @Param("today") LocalDate today, @Param("done") com.sunsetbeach.model.MaintenanceTaskStatus done);

    Optional<RoomUnitBlockEntity> findByIdAndRoomUnitId(String id, String roomUnitId);

    /** Blocks of the given units overlapping [rangeStart, rangeEnd] (both inclusive) - used to render a month's inventory. */
    List<RoomUnitBlockEntity> findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            List<String> roomUnitIds, LocalDate rangeEnd, LocalDate rangeStart);

    /**
     * Blocks of every unit (any room type) overlapping [rangeStart, rangeEnd] (both inclusive) -
     * used by {@code GET /bookings/calendar}, which spans every room type at once.
     */
    List<RoomUnitBlockEntity> findByFromDateLessThanEqualAndToDateGreaterThanEqual(LocalDate rangeEnd, LocalDate rangeStart);

    /**
     * Blocks of one unit overlapping a booking's half-open stay [checkIn, checkOut) - used by
     * the room-assignment race check in {@link com.sunsetbeach.service.BookingWriter}.
     */
    List<RoomUnitBlockEntity> findByRoomUnitIdAndFromDateLessThanAndToDateGreaterThanEqual(
            String roomUnitId, LocalDate checkOut, LocalDate checkIn);
}
