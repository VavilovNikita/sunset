package com.sunsetbeach.repository;

import com.sunsetbeach.entity.SpaAppointmentOrderEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaAppointmentOrderRepository extends JpaRepository<SpaAppointmentOrderEntity, String> {

    List<SpaAppointmentOrderEntity> findBySpaAppointmentId(String spaAppointmentId);

    List<SpaAppointmentOrderEntity> findBySpaAppointmentIdIn(Collection<String> spaAppointmentIds);

    List<SpaAppointmentOrderEntity> findByOrderIdIn(Collection<String> orderIds);
}
