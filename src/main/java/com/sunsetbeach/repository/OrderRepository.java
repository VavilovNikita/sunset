package com.sunsetbeach.repository;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.model.OrderStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {

    boolean existsByTableIdAndStatusIn(String tableId, Collection<OrderStatus> statuses);

    boolean existsByStatusIn(Collection<OrderStatus> statuses);
}
