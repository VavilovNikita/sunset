package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterEntryEntity;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterEntryRepository extends JpaRepository<RosterEntryEntity, String> {

    List<RosterEntryEntity> findByDateBetween(LocalDate from, LocalDate to);

    List<RosterEntryEntity> findByEmployeeUserIdAndDateBetween(String employeeUserId, LocalDate from, LocalDate to);

    /** The precondition every move/reassign/swap checks: does this employee already have an entry that day. */
    Optional<RosterEntryEntity> findByEmployeeUserIdAndDate(String employeeUserId, LocalDate date);

    /** The entries an edited shift code carries forward to its new version - see ShiftCodeService#createVersion. */
    List<RosterEntryEntity> findByShiftCodeIdInAndDateGreaterThanEqual(Collection<String> shiftCodeIds, LocalDate from);

    boolean existsByShiftCodeIdAndDateBefore(String shiftCodeId, LocalDate before);

    /** The entry's row locked until the transaction ends - a decision on a shift request validates and applies under it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RosterEntryEntity e where e.id = :id")
    Optional<RosterEntryEntity> findByIdForUpdate(@Param("id") String id);
}
