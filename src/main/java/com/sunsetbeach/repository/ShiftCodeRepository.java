package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.model.StaffArea;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftCodeRepository extends JpaRepository<ShiftCodeEntity, String> {

    List<ShiftCodeEntity> findByActiveTrue();

    List<ShiftCodeEntity> findByStaffAreaAndActiveTrue(StaffArea staffArea);

    /** The shared (not area-scoped) half of the resolved view {@link com.sunsetbeach.service.ShiftCodeService#list} builds for one area. */
    List<ShiftCodeEntity> findByStaffAreaIsNullAndActiveTrue();

    /**
     * The row a new version of the same code retires - see V57's own comment. {@code staffArea}
     * may be null (a shared code); Spring Data JPA compiles a null-valued equality parameter to
     * {@code IS NULL} rather than {@code = NULL} (which would never match), so this correctly
     * retires only the previous version of the *same* scope - a shared and an area-scoped row
     * sharing a {@code code} string never retire each other.
     */
    Optional<ShiftCodeEntity> findByStaffAreaAndCodeAndActiveTrue(StaffArea staffArea, String code);
}
