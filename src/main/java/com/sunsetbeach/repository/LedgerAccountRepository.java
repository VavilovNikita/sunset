package com.sunsetbeach.repository;

import com.sunsetbeach.entity.LedgerAccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, String> {

    List<LedgerAccountEntity> findAllByOrderByCodeAsc();
}
