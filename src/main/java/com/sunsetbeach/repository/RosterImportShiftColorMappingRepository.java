package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterImportShiftColorMappingEntity;
import com.sunsetbeach.model.FillColor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterImportShiftColorMappingRepository extends JpaRepository<RosterImportShiftColorMappingEntity, String> {

    Optional<RosterImportShiftColorMappingEntity> findByRawCodeAndFillColor(String rawCode, FillColor fillColor);
}
