package com.sunsetbeach.repository;

import com.sunsetbeach.entity.TelegramBotStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramBotStateRepository extends JpaRepository<TelegramBotStateEntity, Integer> {
}
