package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterChangeMarkEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RosterChangeMarkRepository extends JpaRepository<RosterChangeMarkEntity, String> {

    /**
     * The first change of a burst writes the mark with the state as it was; every later one only moves
     * {@code lastChangedAt} - the unique (employee, day) index decides which is which.
     */
    @Modifying
    @Query(value = """
            INSERT INTO "RosterChangeMark" (id, "employeeUserId", "shiftDate", "beforeShiftCodeId", "firstChangedAt", "lastChangedAt")
            VALUES (:id, :employeeUserId, :shiftDate, :beforeShiftCodeId, :now, :now)
            ON CONFLICT ("employeeUserId", "shiftDate") DO UPDATE SET "lastChangedAt" = EXCLUDED."lastChangedAt"
            """, nativeQuery = true)
    int touch(@Param("id") String id, @Param("employeeUserId") String employeeUserId, @Param("shiftDate") LocalDate shiftDate,
              @Param("beforeShiftCodeId") String beforeShiftCodeId, @Param("now") LocalDateTime now);

    List<RosterChangeMarkEntity> findByLastChangedAtLessThanEqual(LocalDateTime cutoff);

    /** Deletes the mark only if it was not touched again meanwhile (a change that landed during the sweep keeps it for the next one). */
    @Modifying
    @Query("delete from RosterChangeMarkEntity m where m.id = :id and m.lastChangedAt = :lastChangedAt")
    int deleteIfUnchanged(@Param("id") String id, @Param("lastChangedAt") LocalDateTime lastChangedAt);
}
