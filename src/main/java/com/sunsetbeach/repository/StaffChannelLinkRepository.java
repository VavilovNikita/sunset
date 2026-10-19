package com.sunsetbeach.repository;

import com.sunsetbeach.entity.StaffChannelLinkEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StaffChannelLinkRepository extends JpaRepository<StaffChannelLinkEntity, String> {

    Optional<StaffChannelLinkEntity> findByUserIdAndChannel(String userId, String channel);

    Optional<StaffChannelLinkEntity> findByChannelAndExternalId(String channel, String externalId);

    List<StaffChannelLinkEntity> findByUserId(String userId);

    List<StaffChannelLinkEntity> findByUserIdIn(Collection<String> userIds);

    List<StaffChannelLinkEntity> findByChannel(String channel);

    /** A plain UPDATE of this one column, so it never overwrites anything else a concurrent change wrote to the row. */
    @Transactional
    @Modifying
    @Query("update StaffChannelLinkEntity l set l.menuSignature = :signature where l.id = :id")
    int setMenuSignature(@Param("id") String id, @Param("signature") String signature);
}
