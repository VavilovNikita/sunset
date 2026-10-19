package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every status change is one conditional UPDATE ({@code ... WHERE status = 'PENDING'}): the row count says
 * who won, so two deciders pressing at once (or a decision racing the expiry sweep) change it once.
 */
public interface ShiftChangeRequestRepository extends JpaRepository<ShiftChangeRequestEntity, String>, JpaSpecificationExecutor<ShiftChangeRequestEntity> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE "ShiftChangeRequest"
               SET status = :status, "decidedByUserId" = :decidedBy, "decidedAt" = :now, "decisionComment" = :comment, "updatedAt" = :now
             WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int decide(@Param("id") String id, @Param("status") String status, @Param("decidedBy") String decidedBy,
               @Param("comment") String comment, @Param("now") LocalDateTime now);

    /** Nothing was applied; our own words say why. Same guard as {@link #decide}. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE "ShiftChangeRequest"
               SET status = 'EXPIRED', "expiredReason" = :reason, "decidedByUserId" = :decidedBy, "decidedAt" = :now, "updatedAt" = :now
             WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int expire(@Param("id") String id, @Param("reason") String reason, @Param("decidedBy") String decidedBy, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE "ShiftChangeRequest"
               SET status = 'CANCELLED', "decidedByUserId" = :by, "decidedAt" = :now, "updatedAt" = :now
             WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int cancel(@Param("id") String id, @Param("by") String by, @Param("now") LocalDateTime now);

    List<ShiftChangeRequestEntity> findByStatus(String status);

    List<ShiftChangeRequestEntity> findByRosterEntryIdInAndStatus(Collection<String> entryIds, String status);

    List<ShiftChangeRequestEntity> findByEmployeeUserIdOrderByCreatedAtDesc(String employeeUserId, Pageable page);
}
