package com.sunsetbeach.repository;

import com.sunsetbeach.entity.AttendancePunchEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePunchRepository extends JpaRepository<AttendancePunchEntity, String> {

    List<AttendancePunchEntity> findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(
            String employeeUserId, LocalDateTime from, LocalDateTime to);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndEnrollmentNumberAndPunchAt(String deviceId, int enrollmentNumber, LocalDateTime punchAt);
}
