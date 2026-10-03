package com.sunsetbeach.repository;

import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.model.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {

    boolean existsByTableIdAndStatusIn(String tableId, Collection<OrderStatus> statuses);

    boolean existsByStatusIn(Collection<OrderStatus> statuses);

    List<OrderEntity> findByTableIdIsNotNullAndStatusIn(Collection<OrderStatus> statuses);

    /**
     * {@code SELECT ... FOR UPDATE} - {@code OrderService#voidItem} and {@code #close} both take it,
     * so a void can't change the lines/total between a concurrent close reading the total and
     * charging it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") String id);
}
