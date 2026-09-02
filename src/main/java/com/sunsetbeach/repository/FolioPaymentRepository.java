package com.sunsetbeach.repository;

import com.sunsetbeach.entity.FolioPaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolioPaymentRepository extends JpaRepository<FolioPaymentEntity, String> {

    List<FolioPaymentEntity> findByBookingIdOrderByCreatedAtAsc(String bookingId);
}
