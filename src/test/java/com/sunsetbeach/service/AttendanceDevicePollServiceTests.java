package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.attendance.AttendanceDeviceException;
import com.sunsetbeach.attendance.FakeZkTerminalClient;
import com.sunsetbeach.attendance.RawAttendancePunch;
import com.sunsetbeach.attendance.ZkTerminalClient;
import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * {@code AttendanceDevicePollService}'s own orchestration - {@code lastSeenAt}, per-device
 * independence (one device failing must not stop another from being polled), and unattributable
 * punches being skipped rather than crashing the poll - built and tested entirely against {@link
 * FakeZkTerminalClient} (see {@code ZkTerminalClient}'s own javadoc), swapped in for the real
 * bean the same way {@code SpaMapServiceTests} swaps in a fixed {@code Clock}: a {@code @Primary}
 * bean in a nested {@code @TestConfiguration}.
 */
@SpringBootTest
class AttendanceDevicePollServiceTests extends AbstractIntegrationTest {

    @TestConfiguration
    static class FakeClientConfig {
        @Bean
        @Primary
        FakeZkTerminalClient fakeZkTerminalClient() {
            return new FakeZkTerminalClient();
        }
    }

    @Autowired
    private AttendanceDevicePollService attendanceDevicePollService;

    @Autowired
    private ZkTerminalClient zkTerminalClient;

    @Autowired
    private AttendanceDeviceRepository attendanceDeviceRepository;

    @Autowired
    private AttendancePunchRepository attendancePunchRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<String> createdDeviceIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream().filter(p -> createdUserIds.contains(p.getEmployeeUserId())).toList());
        createdUserIds.forEach(userRepository::deleteById);
        attendanceDeviceRepository.deleteAllById(createdDeviceIds);
    }

    private FakeZkTerminalClient fake() {
        return (FakeZkTerminalClient) zkTerminalClient;
    }

    private AttendanceDeviceEntity createDevice() {
        AttendanceDeviceEntity device = new AttendanceDeviceEntity();
        device.setName("Poll Test Terminal " + UUID.randomUUID());
        device.setSerial("SN-" + UUID.randomUUID());
        device.setAddress("127.0.0.1");
        device.setTimezone("Asia/Bangkok");
        AttendanceDeviceEntity saved = attendanceDeviceRepository.saveAndFlush(device);
        createdDeviceIds.add(saved.getId());
        return saved;
    }

    private UserEntity createEnrolledUser(int enrollmentNumber) {
        UserEntity user = new UserEntity();
        user.setName("Poll Test Employee " + UUID.randomUUID());
        user.setRole(Role.WAITER);
        user.setActive(true);
        user.setEnrollmentNumber(enrollmentNumber);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private static int uniqueEnrollmentNumber() {
        return Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1;
    }

    @Test
    void pollDevices_successfulPoll_setsLastSeenAtAndIngestsPunches() {
        AttendanceDeviceEntity device = createDevice();
        int enrollmentNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(enrollmentNumber);
        LocalDate date = LocalDate.of(2027, 9, 1);
        fake().queue(
                device.getId(),
                List.of(
                        new RawAttendancePunch(enrollmentNumber, date.atTime(9, 0), PunchDirection.IN),
                        new RawAttendancePunch(enrollmentNumber, date.atTime(18, 0), PunchDirection.OUT)));

        attendanceDevicePollService.pollDevices();

        AttendanceDeviceEntity reloaded = attendanceDeviceRepository.findById(device.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenAt()).isNotNull();
        assertThat(attendancePunchRepository
                        .findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .hasSize(2);
    }

    @Test
    void pollDevices_deviceUnreachable_leavesLastSeenAtUntouched() {
        AttendanceDeviceEntity device = createDevice();
        fake().queue(device.getId(), new AttendanceDeviceException("simulated: unreachable"));

        attendanceDevicePollService.pollDevices();

        AttendanceDeviceEntity reloaded = attendanceDeviceRepository.findById(device.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenAt()).isNull();
    }

    /** The whole point of a per-device sweep: one bad terminal must not stop the others from being read. */
    @Test
    void pollDevices_oneDeviceFails_anotherStillPolledSuccessfully() {
        AttendanceDeviceEntity failing = createDevice();
        AttendanceDeviceEntity working = createDevice();
        int enrollmentNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(enrollmentNumber);
        LocalDate date = LocalDate.of(2027, 9, 2);
        fake().queue(failing.getId(), new AttendanceDeviceException("simulated: unreachable"));
        fake().queue(working.getId(), List.of(new RawAttendancePunch(enrollmentNumber, date.atTime(9, 0), PunchDirection.IN)));

        attendanceDevicePollService.pollDevices();

        assertThat(attendanceDeviceRepository.findById(failing.getId()).orElseThrow().getLastSeenAt()).isNull();
        assertThat(attendanceDeviceRepository.findById(working.getId()).orElseThrow().getLastSeenAt()).isNotNull();
        assertThat(attendancePunchRepository
                        .findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .hasSize(1);
    }

    /** An unattributable punch (no matching enrollment number) is skipped, not a reason to fail the whole poll. */
    @Test
    void pollDevices_unknownEnrollmentNumber_isSkippedNotFatal() {
        AttendanceDeviceEntity device = createDevice();
        int knownNumber = uniqueEnrollmentNumber();
        int unknownNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(knownNumber);
        LocalDate date = LocalDate.of(2027, 9, 3);
        fake().queue(
                device.getId(),
                List.of(
                        new RawAttendancePunch(unknownNumber, date.atTime(8, 0), PunchDirection.IN),
                        new RawAttendancePunch(knownNumber, date.atTime(9, 0), PunchDirection.IN)));

        attendanceDevicePollService.pollDevices();

        assertThat(attendanceDeviceRepository.findById(device.getId()).orElseThrow().getLastSeenAt()).isNotNull();
        assertThat(attendancePunchRepository
                        .findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .hasSize(1);
    }

    @Test
    void pollDevices_inactiveDevice_isNotPolledAtAll() {
        AttendanceDeviceEntity device = createDevice();
        device.setActive(false);
        attendanceDeviceRepository.saveAndFlush(device);
        int before = fake().pollCount();

        attendanceDevicePollService.pollDevices();

        assertThat(fake().pollCount()).isEqualTo(before);
    }

    /** Re-polling the same window again (a real re-send, or just the next cycle re-reading the whole log) must not duplicate rows. */
    @Test
    void pollDevices_samePunchAcrossTwoPollCycles_landsOnce() {
        AttendanceDeviceEntity device = createDevice();
        int enrollmentNumber = uniqueEnrollmentNumber();
        UserEntity employee = createEnrolledUser(enrollmentNumber);
        LocalDate date = LocalDate.of(2027, 9, 4);
        RawAttendancePunch punch = new RawAttendancePunch(enrollmentNumber, date.atTime(9, 0), PunchDirection.IN);
        fake().queue(device.getId(), List.of(punch));
        fake().queue(device.getId(), List.of(punch));

        attendanceDevicePollService.pollDevices();
        attendanceDevicePollService.pollDevices();

        assertThat(attendancePunchRepository
                        .findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .hasSize(1);
    }
}
