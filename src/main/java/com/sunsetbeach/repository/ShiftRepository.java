package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.model.ShiftStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<ShiftEntity, String>, JpaSpecificationExecutor<ShiftEntity> {

    Optional<ShiftEntity> findByOpenedByUserIdAndStatus(String openedByUserId, ShiftStatus status);

    /**
     * The caller's open shift, share-locked for a payment being recorded into it (order close,
     * folio payment). Payments into one shift don't wait for each other; closing the shift
     * ({@link #findByIdForUpdate}) waits for them to commit, so the Z-report it prints counts
     * every one - and a payment that arrives after the close finds no open shift.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from ShiftEntity s where s.openedByUserId = :userId and s.status = :status")
    Optional<ShiftEntity> findByOpenerAndStatusForPayment(@Param("userId") String openedByUserId, @Param("status") ShiftStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShiftEntity s where s.id = :id")
    Optional<ShiftEntity> findByIdForUpdate(@Param("id") String id);
}
