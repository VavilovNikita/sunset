package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RoomUnitEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomUnitRepository extends JpaRepository<RoomUnitEntity, String> {

    List<RoomUnitEntity> findByRoomId(String roomId);

    List<RoomUnitEntity> findByRoomIdAndIsActiveTrue(String roomId);

    long countByRoomIdAndIsActiveTrue(String roomId);

    /** Bulk version of {@link #countByRoomIdAndIsActiveTrue} for {@code RoomService.list()} - avoids one query per room. */
    @Query("SELECT u.roomId AS roomId, COUNT(u) AS activeCount FROM RoomUnitEntity u WHERE u.isActive = true GROUP BY u.roomId")
    List<RoomActiveUnitCount> countActiveGroupedByRoom();

    interface RoomActiveUnitCount {
        String getRoomId();

        long getActiveCount();
    }
}
