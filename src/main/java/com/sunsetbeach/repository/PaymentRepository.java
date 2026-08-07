package com.sunsetbeach.repository;

import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.model.PaymentMethod;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findByShiftId(String shiftId);

    List<PaymentEntity> findByBookingIdAndMethod(String bookingId, PaymentMethod method);

    /** Half-open [from, toExclusive) - callers pass toExclusive = the day after the last inclusive day. */
    List<PaymentEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);
}
