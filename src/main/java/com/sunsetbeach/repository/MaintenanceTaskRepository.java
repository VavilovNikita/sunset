package com.sunsetbeach.repository;

import com.sunsetbeach.entity.MaintenanceTaskEntity;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import java.util.Collection;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTaskEntity, String> {

    List<MaintenanceTaskEntity> findAllByOrderByCreatedAtDesc();

    /**
     * The task's row locked until the transaction ends. A status change reads the status and then writes it
     * (and, for DONE, shortens the block and audits): two people closing the same task at once - the bot and the
     * admin screen, two engineers - must not both pass the "is this a forward move" check on the same old status.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MaintenanceTaskEntity t where t.id = :id")
    Optional<MaintenanceTaskEntity> findByIdForUpdate(@Param("id") String id);

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
