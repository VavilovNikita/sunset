package com.sunsetbeach.repository;

import com.sunsetbeach.entity.StaffChannelLinkEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffChannelLinkRepository extends JpaRepository<StaffChannelLinkEntity, String> {

    Optional<StaffChannelLinkEntity> findByUserIdAndChannel(String userId, String channel);

    Optional<StaffChannelLinkEntity> findByChannelAndExternalId(String channel, String externalId);

    List<StaffChannelLinkEntity> findByUserId(String userId);

    List<StaffChannelLinkEntity> findByUserIdIn(Collection<String> userIds);
}
