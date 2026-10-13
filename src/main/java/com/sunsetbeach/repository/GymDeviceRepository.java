package com.sunsetbeach.repository;

import com.sunsetbeach.entity.GymDeviceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymDeviceRepository extends JpaRepository<GymDeviceEntity, String> {

    List<GymDeviceEntity> findAllByOrderBySortOrderAscNameAsc();

    List<GymDeviceEntity> findByActiveTrueOrderBySortOrderAscNameAsc();

    boolean existsByTuyaDeviceId(String tuyaDeviceId);

    boolean existsByTuyaDeviceIdAndIdNot(String tuyaDeviceId, String id);
}
