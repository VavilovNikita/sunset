package com.sunsetbeach.repository;

import com.sunsetbeach.entity.FolioPaymentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolioPaymentRepository extends JpaRepository<FolioPaymentEntity, String> {

    List<FolioPaymentEntity> findByBookingIdOrderByCreatedAtAsc(String bookingId);

    /** Taken at reception during this cash shift - see {@code ShiftService#folioTotals}. */
    List<FolioPaymentEntity> findByShiftIdOrderByCreatedAtAsc(String shiftId);

    /** GET /reports/revenue-export - {@code createdAt} is UTC wall-clock, so callers pass a UTC window. */
    List<FolioPaymentEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);
}
