package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ChannelLinkCodeEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelLinkCodeRepository extends JpaRepository<ChannelLinkCodeEntity, String> {

    /**
     * Spends a code: succeeds (1) only while it is unused and unexpired. A conditional UPDATE, so
     * two chats opening the same link at once cannot both win.
     */
    @Modifying
    @Query("update ChannelLinkCodeEntity c set c.usedAt = :now where c.code = :code and c.usedAt is null and c.expiresAt > :now")
    int consume(@Param("code") String code, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from ChannelLinkCodeEntity c where c.userId = :userId and c.channel = :channel and c.usedAt is null")
    int deleteUnusedFor(@Param("userId") String userId, @Param("channel") String channel);

    @Modifying
    @Query("delete from ChannelLinkCodeEntity c where c.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
