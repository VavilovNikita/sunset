package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ManualUpdateEntryEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ManualUpdateEntryRepository extends JpaRepository<ManualUpdateEntryEntity, String> {

    /**
     * Held until the caller's transaction ends, so "has this day had its message" and "write that it has" cannot be
     * passed by two passes at once. Cast to text only because {@code pg_advisory_xact_lock} returns void.
     */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtext('manual-update-announcement')) AS text)", nativeQuery = true)
    String lockAnnouncements();

    boolean existsBySentOn(LocalDate sentOn);
}
