package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.model.StaffArea;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftCodeRepository extends JpaRepository<ShiftCodeEntity, String> {

    List<ShiftCodeEntity> findByActiveTrue();

    List<ShiftCodeEntity> findByStaffAreaAndActiveTrue(StaffArea staffArea);

    /** The row a new version of the same code retires - see V57's own comment. */
    Optional<ShiftCodeEntity> findByStaffAreaAndCodeAndActiveTrue(StaffArea staffArea, String code);
}
