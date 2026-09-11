package com.sunsetbeach.repository;

import com.sunsetbeach.entity.SpaAppointmentTreatmentEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaAppointmentTreatmentRepository extends JpaRepository<SpaAppointmentTreatmentEntity, String> {

    List<SpaAppointmentTreatmentEntity> findBySpaAppointmentId(String spaAppointmentId);

    /** The day grid's batched read - one query for every appointment on the date, not one per row. */
    List<SpaAppointmentTreatmentEntity> findBySpaAppointmentIdIn(Collection<String> spaAppointmentIds);

    long countBySpaAppointmentId(String spaAppointmentId);
}
