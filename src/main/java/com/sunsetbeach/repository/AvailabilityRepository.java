package com.sunsetbeach.repository;

import com.sunsetbeach.entity.AvailabilityEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<AvailabilityEntity, String> {

    Optional<AvailabilityEntity> findByRoomIdAndDate(String roomId, LocalDate date);

    List<AvailabilityEntity> findByRoomIdAndDateBetween(String roomId, LocalDate start, LocalDate end);

    /** Used by {@link com.sunsetbeach.service.RoomService} to find the peak future commitment before shrinking `quantity`. */
    List<AvailabilityEntity> findByRoomIdAndDateGreaterThanEqual(String roomId, LocalDate from);
}
