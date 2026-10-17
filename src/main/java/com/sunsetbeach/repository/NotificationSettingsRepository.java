package com.sunsetbeach.repository;

import com.sunsetbeach.entity.NotificationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettingsEntity, Integer> {
}
