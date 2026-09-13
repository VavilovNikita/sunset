package com.sunsetbeach.repository;

import com.sunsetbeach.entity.MaintenanceTaskEntity;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTaskEntity, String> {

    List<MaintenanceTaskEntity> findAllByOrderByCreatedAtDesc();

    /** {@link com.sunsetbeach.service.PropertyMapService}'s per-unit "open task" lookup - every OPEN/IN_PROGRESS task across all units, not paged, matching how it already loads blocks/bookings for the whole map in one go. */
    List<MaintenanceTaskEntity> findByStatusIn(List<MaintenanceTaskStatus> statuses);

    /**
     * The reverse of {@link MaintenanceTaskEntity#getBlockId()} - given a batch of block ids
     * (the calendar's own, already-loaded set), find whichever tasks link to any of them, any
     * status, in one query. {@link com.sunsetbeach.service.BookingCalendarService}'s own block
     * panel needs this; nothing before it needed to walk this relationship in reverse at all.
     */
    List<MaintenanceTaskEntity> findByBlockIdIn(Collection<String> blockIds);
}
