package com.sunsetbeach.repository;

import com.sunsetbeach.entity.OrderItemVoidEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemVoidRepository extends JpaRepository<OrderItemVoidEntity, String> {

    List<OrderItemVoidEntity> findByOrderIdOrderByVoidedAtAsc(String orderId);

    List<OrderItemVoidEntity> findByOrderIdInOrderByVoidedAtAsc(Collection<String> orderIds);
}
