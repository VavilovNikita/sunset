package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterImportNameMappingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterImportNameMappingRepository extends JpaRepository<RosterImportNameMappingEntity, String> {

    Optional<RosterImportNameMappingEntity> findByRawName(String rawName);
}
