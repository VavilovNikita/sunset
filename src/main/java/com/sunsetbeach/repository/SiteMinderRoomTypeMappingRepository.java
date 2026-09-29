package com.sunsetbeach.repository;

import com.sunsetbeach.entity.SiteMinderRoomTypeMappingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteMinderRoomTypeMappingRepository extends JpaRepository<SiteMinderRoomTypeMappingEntity, String> {

    /** Case-insensitive, matching V123's unique index on lower("siteMinderRoomType"). Callers pass an already whitespace-normalized name. */
    @Query("select m from SiteMinderRoomTypeMappingEntity m where lower(m.siteMinderRoomType) = lower(:name)")
    Optional<SiteMinderRoomTypeMappingEntity> findByName(@Param("name") String name);

    List<SiteMinderRoomTypeMappingEntity> findAllByOrderBySiteMinderRoomTypeAsc();
}
