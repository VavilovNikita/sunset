package com.sunsetbeach.repository;

import com.sunsetbeach.entity.GymReservationActionEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymReservationActionRepository extends JpaRepository<GymReservationActionEntity, String> {

    List<GymReservationActionEntity> findByReservationIdAndPhaseOrderByDeviceNameAsc(String reservationId, String phase);

    List<GymReservationActionEntity> findByReservationIdIn(Collection<String> reservationIds);

    boolean existsByReservationId(String reservationId);

    boolean existsByReservationIdAndPhase(String reservationId, String phase);
}
