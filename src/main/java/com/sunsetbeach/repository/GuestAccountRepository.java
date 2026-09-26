package com.sunsetbeach.repository;

import com.sunsetbeach.entity.GuestAccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestAccountRepository extends JpaRepository<GuestAccountEntity, String> {

    /** email is always stored lowercased (see GuestAccountEntity's own comment) - callers must lowercase first. */
    Optional<GuestAccountEntity> findByEmail(String email);

    Optional<GuestAccountEntity> findByEmailVerificationToken(String emailVerificationToken);

    Optional<GuestAccountEntity> findByGuestId(String guestId);

    boolean existsByGuestId(String guestId);
}
