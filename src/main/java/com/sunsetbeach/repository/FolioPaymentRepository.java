package com.sunsetbeach.repository;

import com.sunsetbeach.entity.FolioPaymentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolioPaymentRepository extends JpaRepository<FolioPaymentEntity, String> {

    List<FolioPaymentEntity> findByBookingIdOrderByCreatedAtAsc(String bookingId);

    /** GET /reports/revenue-export - {@code createdAt} is UTC wall-clock, so callers pass a UTC window. */
    List<FolioPaymentEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);
}
