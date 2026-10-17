package com.sunsetbeach.repository;

import com.sunsetbeach.entity.NotificationEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String>, JpaSpecificationExecutor<NotificationEntity> {

    /**
     * Inserts unless this (dedupKey, recipient) already exists - the unique index decides, so two
     * scanners racing on the same condition raise it once. Returns 1 when inserted, 0 when it was there.
     */
    @Modifying
    @Query(value = """
            INSERT INTO "Notification" (id, type, "messageKey", "recipientUserId", language, text, "dedupKey", "createdAt")
            VALUES (:id, :type, :messageKey, :recipientUserId, :language, :text, :dedupKey, :createdAt)
            ON CONFLICT ("dedupKey", "recipientUserId") DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("type") String type,
            @Param("messageKey") String messageKey,
            @Param("recipientUserId") String recipientUserId,
            @Param("language") String language,
            @Param("text") String text,
            @Param("dedupKey") String dedupKey,
            @Param("createdAt") LocalDateTime createdAt);

    List<NotificationEntity> findByDedupKeyStartingWith(String prefix);

    /** Retention: deliveries go with their notification (ON DELETE CASCADE). Returns the rows removed. */
    @Modifying
    @Query("delete from NotificationEntity n where n.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByDedupKeyAndRecipientUserId(String dedupKey, String recipientUserId);
}
