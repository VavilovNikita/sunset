package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RoomUnitEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RoomUnitRepository extends JpaRepository<RoomUnitEntity, String> {

    List<RoomUnitEntity> findByRoomId(String roomId);

    /** Row lock on one physical room: check-in takes it so two arrivals can't both find the room empty. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from RoomUnitEntity u where u.id = :id")
    Optional<RoomUnitEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") String id);

    /**
     * Rewrites the row unchanged, from a check-in holding its lock. A {@code SERIALIZABLE} move into this room whose
     * snapshot is older than that check-in then fails with a serialization error ("try again") when it locks the row,
     * instead of reading the room as still empty.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE \"RoomUnit\" SET \"label\" = \"label\" WHERE id = :id", nativeQuery = true)
    void markUsed(@org.springframework.data.repository.query.Param("id") String id);

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
