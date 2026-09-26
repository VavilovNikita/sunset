package com.sunsetbeach.repository;

import com.sunsetbeach.entity.GuestEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<GuestEntity, String> {

    /**
     * Case-insensitive substring match against name, email, or phone - one field matching is
     * enough, not all three. Backs {@code GET /guests?q=}, including its client-side reuse for
     * seeding the booking-detail/check-in link action from a booking's own snapshot fields and
     * for the create-time duplicate warning - see the operation's own description in
     * openapi.yaml.
     */
    @Query("""
        SELECT g FROM GuestEntity g
        WHERE :pattern IS NULL
           OR LOWER(g.name) LIKE :pattern
           OR LOWER(g.email) LIKE :pattern
           OR LOWER(g.phone) LIKE :pattern
        ORDER BY g.createdAt DESC
        """)
    List<GuestEntity> search(@Param("pattern") String pattern);

    /** Exact match on trimmed, lowercased email - {@code normalizedEmail} must already be both. Can return several rows: Guest.email has no unique constraint. */
    @Query("SELECT g FROM GuestEntity g WHERE LOWER(TRIM(g.email)) = :normalizedEmail")
    List<GuestEntity> findByNormalizedEmail(@Param("normalizedEmail") String normalizedEmail);

    /**
     * Transaction-scoped advisory lock on one normalized email - see
     * {@code GuestLinkService#findOrCreate} for why. Released automatically at commit/rollback.
     * Cast to text only because {@code pg_advisory_xact_lock} returns void, which Hibernate has no
     * JDBC mapping for.
     */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtext('guest-email:' || :normalizedEmail)) AS text)", nativeQuery = true)
    String lockEmail(@Param("normalizedEmail") String normalizedEmail);
}
