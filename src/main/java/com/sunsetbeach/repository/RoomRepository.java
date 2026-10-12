package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<RoomEntity, String> {

    /**
     * Whether some room type other than {@code excludeId} (null on create) already has this name,
     * compared the way the V138 unique index does: ignoring case and spaces at either end.
     */
    @Query("select count(r) > 0 from RoomEntity r where lower(trim(r.name)) = lower(trim(:name)) and (:excludeId is null or r.id <> :excludeId)")
    boolean existsByNormalizedName(@Param("name") String name, @Param("excludeId") String excludeId);
}
