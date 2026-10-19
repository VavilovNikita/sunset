package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BotConversationEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BotConversationRepository extends JpaRepository<BotConversationEntity, String> {

    @Modifying
    @Query("delete from BotConversationEntity c where c.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
