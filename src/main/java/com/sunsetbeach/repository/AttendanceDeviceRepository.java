package com.sunsetbeach.repository;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceDeviceRepository extends JpaRepository<AttendanceDeviceEntity, String> {

    List<AttendanceDeviceEntity> findByActiveTrue();
}
