package com.sunsetbeach.repository;

import com.sunsetbeach.entity.LightingDeviceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LightingDeviceRepository extends JpaRepository<LightingDeviceEntity, String> {

    List<LightingDeviceEntity> findAllByOrderBySortOrderAscNameAsc();

    List<LightingDeviceEntity> findByActiveTrueOrderBySortOrderAscNameAsc();

    /** Whether another controller (not {@code excludeId}; null on create) has this name under V146's rule: ignoring case and spaces. */
    @Query("select count(d) > 0 from LightingDeviceEntity d where function('sunset_normalize_name', d.name) = function('sunset_normalize_name', :name) and (:excludeId is null or d.id <> :excludeId)")
    boolean existsByNormalizedName(@Param("name") String name, @Param("excludeId") String excludeId);
}
