package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RoomUnitEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomUnitRepository extends JpaRepository<RoomUnitEntity, String> {

    List<RoomUnitEntity> findByRoomId(String roomId);

    List<RoomUnitEntity> findByRoomIdAndIsActiveTrue(String roomId);

    /** Whether another room (not {@code excludeId}; null on create) has this label under V143's rule: ignoring case and spaces. */
    @Query("select count(u) > 0 from RoomUnitEntity u where function('sunset_normalize_name', u.label) = function('sunset_normalize_name', :label) and (:excludeId is null or u.id <> :excludeId)")
    boolean existsByNormalizedLabel(@org.springframework.data.repository.query.Param("label") String label, @org.springframework.data.repository.query.Param("excludeId") String excludeId);

    long countByRoomIdAndIsActiveTrue(String roomId);

    /** Bulk version of {@link #countByRoomIdAndIsActiveTrue} for {@code RoomService.list()} - avoids one query per room. */
    @Query("SELECT u.roomId AS roomId, COUNT(u) AS activeCount FROM RoomUnitEntity u WHERE u.isActive = true GROUP BY u.roomId")
    List<RoomActiveUnitCount> countActiveGroupedByRoom();

    interface RoomActiveUnitCount {
        String getRoomId();

        long getActiveCount();
    }
}
