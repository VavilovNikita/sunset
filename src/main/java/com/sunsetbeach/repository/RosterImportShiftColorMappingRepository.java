package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterImportShiftColorMappingEntity;
import com.sunsetbeach.model.FillColor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterImportShiftColorMappingRepository extends JpaRepository<RosterImportShiftColorMappingEntity, String> {

    Optional<RosterImportShiftColorMappingEntity> findByRawCodeAndFillColor(String rawCode, FillColor fillColor);

    /**
     * What {@code ShiftCodeService#computeSuggestedColorHex} reads for "9"/"9S" - {@code
     * resolvedCode} isn't a unique key by schema (only {@code (rawCode, fillColor)} is, see
     * V78/V81's own comments), but in practice at most one row resolves to each of "9"/"9S", so
     * this is a plain lookup, not a batch read.
     */
    List<RosterImportShiftColorMappingEntity> findByResolvedCode(String resolvedCode);
}
