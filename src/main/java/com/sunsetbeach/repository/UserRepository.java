package com.sunsetbeach.repository;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByRoleIn(Collection<Role> roles);

    Optional<UserEntity> findByEnrollmentNumber(Integer enrollmentNumber);

    /**
     * Transaction-scoped advisory lock serialising every change that could give a second active user the
     * HR function (the set lives in a TEXT[] column, so no index can say "at most one"). Cast to text
     * only because {@code pg_advisory_xact_lock} returns void.
     */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtext('hr-function-holder')) AS text)", nativeQuery = true)
    String lockHrAssignment();

    @Query(value = "SELECT * FROM \"User\" WHERE \"isActive\" AND 'HR' = ANY (\"jobFunctions\") ORDER BY name", nativeQuery = true)
    List<UserEntity> findActiveHrHolders();
}
