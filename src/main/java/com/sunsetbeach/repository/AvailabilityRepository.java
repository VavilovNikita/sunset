package com.sunsetbeach.repository;

import com.sunsetbeach.entity.AvailabilityEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<AvailabilityEntity, String> {

    Optional<AvailabilityEntity> findByRoomIdAndDate(String roomId, LocalDate date);

    List<AvailabilityEntity> findByRoomIdAndDateBetween(String roomId, LocalDate start, LocalDate end);

    long countByRoomIdAndIsBlockedTrueAndDateGreaterThanEqualAndDateLessThan(
            String roomId, LocalDate fromInclusive, LocalDate toExclusive);
}
