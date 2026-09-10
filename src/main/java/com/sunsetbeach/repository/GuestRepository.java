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
}
