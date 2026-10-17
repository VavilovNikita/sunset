package com.sunsetbeach.repository;

import com.sunsetbeach.entity.TerminalSilenceEpisodeEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerminalSilenceEpisodeRepository extends JpaRepository<TerminalSilenceEpisodeEntity, String> {

    /** Opens an episode; 0 when one is already open (another scanner pass got there first). */
    @Modifying
    @Query(value = """
            INSERT INTO "TerminalSilenceEpisode" ("deviceId", "anchorLastSeenAt", "startedAt")
            VALUES (:deviceId, :anchor, :startedAt) ON CONFLICT ("deviceId") DO NOTHING
            """, nativeQuery = true)
    int openIfAbsent(@Param("deviceId") String deviceId, @Param("anchor") LocalDateTime anchor, @Param("startedAt") LocalDateTime startedAt);

    /** Closes an episode; 1 only for the pass that actually removed it, so "back online" is raised once. */
    @Modifying
    @Query("delete from TerminalSilenceEpisodeEntity e where e.deviceId = :deviceId")
    int close(@Param("deviceId") String deviceId);
}
