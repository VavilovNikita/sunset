package com.sunsetbeach.repository;

import com.sunsetbeach.entity.NotificationDeliveryEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, String> {

    @Query("select d from NotificationDeliveryEntity d where d.status = 'PENDING' and d.nextAttemptAt <= :now order by d.nextAttemptAt, d.createdAt")
    List<NotificationDeliveryEntity> findDue(@Param("now") LocalDateTime now, Pageable page);

    List<NotificationDeliveryEntity> findByNotificationIdIn(Collection<String> notificationIds);
}
