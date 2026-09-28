package com.sunsetbeach.repository;

import com.sunsetbeach.entity.NightAuditEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NightAuditRepository extends JpaRepository<NightAuditEntity, String> {

    Optional<NightAuditEntity> findByDate(LocalDate date);
}
