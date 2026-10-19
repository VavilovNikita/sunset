package com.sunsetbeach.repository;

import com.sunsetbeach.entity.StaffActionEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffActionRepository extends JpaRepository<StaffActionEntity, String> {

    List<StaffActionEntity> findByNotificationIdIn(Collection<String> notificationIds);

    @Modifying
    @Query("delete from StaffActionEntity a where a.expiresAt is not null and a.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
