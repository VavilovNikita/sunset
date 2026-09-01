package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.model.ShiftStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShiftRepository extends JpaRepository<ShiftEntity, String>, JpaSpecificationExecutor<ShiftEntity> {

    Optional<ShiftEntity> findByOpenedByUserIdAndStatus(String openedByUserId, ShiftStatus status);
}
