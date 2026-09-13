package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RosterEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterEntryRepository extends JpaRepository<RosterEntryEntity, String> {

    List<RosterEntryEntity> findByDateBetween(LocalDate from, LocalDate to);

    List<RosterEntryEntity> findByEmployeeUserIdAndDateBetween(String employeeUserId, LocalDate from, LocalDate to);

    /** The precondition every move/reassign/swap checks: does this employee already have an entry that day. */
    Optional<RosterEntryEntity> findByEmployeeUserIdAndDate(String employeeUserId, LocalDate date);
}
