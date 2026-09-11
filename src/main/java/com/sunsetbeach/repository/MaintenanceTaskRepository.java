package com.sunsetbeach.repository;

import com.sunsetbeach.entity.MaintenanceTaskEntity;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTaskEntity, String> {

    List<MaintenanceTaskEntity> findAllByOrderByCreatedAtDesc();

    /** {@link com.sunsetbeach.service.PropertyMapService}'s per-unit "open task" lookup - every OPEN/IN_PROGRESS task across all units, not paged, matching how it already loads blocks/bookings for the whole map in one go. */
    List<MaintenanceTaskEntity> findByStatusIn(List<MaintenanceTaskStatus> statuses);
}
