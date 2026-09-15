package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterImportShiftColorMappingEntity;
import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.StaffArea;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterImportShiftColorMappingRepository extends JpaRepository<RosterImportShiftColorMappingEntity, String> {

    Optional<RosterImportShiftColorMappingEntity> findByStaffAreaAndRawCodeAndFillColor(StaffArea staffArea, String rawCode, FillColor fillColor);
}
