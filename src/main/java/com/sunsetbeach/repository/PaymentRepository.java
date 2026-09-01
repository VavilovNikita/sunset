package com.sunsetbeach.repository;

import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.model.PaymentMethod;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findByShiftId(String shiftId);

    List<PaymentEntity> findByBookingIdAndMethod(String bookingId, PaymentMethod method);

    /** At most one row - see the {@code Payment_unique_per_order} constraint. */
    Optional<PaymentEntity> findByOrderId(String orderId);

    List<PaymentEntity> findByOrderIdIn(Collection<String> orderIds);

    /** Half-open [from, toExclusive) - callers pass toExclusive = the day after the last inclusive day. */
    List<PaymentEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime toExclusive);
}
