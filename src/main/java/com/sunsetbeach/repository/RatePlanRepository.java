package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RatePlanEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatePlanRepository extends JpaRepository<RatePlanEntity, String> {

    Optional<RatePlanEntity> findByRoomIdAndDate(String roomId, LocalDate date);

    List<RatePlanEntity> findByRoomIdAndDateBetween(String roomId, LocalDate start, LocalDate end);

    List<RatePlanEntity> findByRoomIdAndDateIn(String roomId, List<LocalDate> dates);
}
