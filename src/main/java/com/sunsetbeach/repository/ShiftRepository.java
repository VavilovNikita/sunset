package com.sunsetbeach.repository;

import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.model.ShiftStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<ShiftEntity, String> {

    Optional<ShiftEntity> findByOpenedByUserIdAndStatus(String openedByUserId, ShiftStatus status);
}
