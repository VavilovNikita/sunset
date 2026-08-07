package com.sunsetbeach.repository;

import com.sunsetbeach.entity.OrderItemEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {

    List<OrderItemEntity> findByOrderId(String orderId);

    List<OrderItemEntity> findByOrderIdIn(Collection<String> orderIds);

    Optional<OrderItemEntity> findByIdAndOrderId(String id, String orderId);
}
