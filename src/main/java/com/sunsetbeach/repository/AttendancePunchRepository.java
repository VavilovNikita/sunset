package com.sunsetbeach.repository;

import com.sunsetbeach.entity.AttendancePunchEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendancePunchRepository extends JpaRepository<AttendancePunchEntity, String> {

    List<AttendancePunchEntity> findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(
            String employeeUserId, LocalDateTime from, LocalDateTime to);

    /**
     * Every employee's punches in a range, not one employee's - the actuals export (unlike {@code
     * summary}) reports across the whole roster at once. Ordered by employee first so a caller can
     * group consecutive rows into one employee's list with the punchAt order inside each group
     * already correct for same-day IN/OUT pairing, without a second sort.
     */
    List<AttendancePunchEntity> findByPunchAtBetweenOrderByEmployeeUserIdAscPunchAtAsc(LocalDateTime from, LocalDateTime to);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndEnrollmentNumberAndPunchAt(String deviceId, int enrollmentNumber, LocalDateTime punchAt);

    /**
     * The device's own newest punch timestamp we've already ingested - the watermark a windowed
     * poll reads forward from (see {@code AttendanceDevicePollService}). Deliberately computed
     * from what's actually stored rather than a separately maintained column on {@code
     * AttendanceDeviceEntity}: a stored watermark is one more thing that could drift from the data
     * it's supposed to summarize, and this query costs nothing extra a poll wasn't already paying
     * for (one indexed lookup, same {@code deviceId} the ingestion uniqueness check already uses).
     */
    @Query("SELECT MAX(p.punchAt) FROM AttendancePunchEntity p WHERE p.deviceId = :deviceId")
    Optional<LocalDateTime> findMaxPunchAtByDeviceId(@Param("deviceId") String deviceId);
}
