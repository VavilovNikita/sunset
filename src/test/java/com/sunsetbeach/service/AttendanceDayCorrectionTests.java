package com.sunsetbeach.service;

import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AttendanceDayCorrectionInput;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@code PUT /attendance/day}: a day's punches are replaced, never edited or deleted - the old ones
 * are voided and stay as history, and every report reads only the live ones. Clock pinned past
 * every date used here, never the wall clock.
 */
@SpringBootTest
class AttendanceDayCorrectionTests extends AbstractIntegrationTest {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
    private static final LocalDate DAY = LocalDate.of(2027, 7, 1);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDate.of(2028, 1, 1).atStartOfDay(HOTEL_ZONE).toInstant(), HOTEL_ZONE);
        }
    }

    @Autowired private AttendanceService attendanceService;
    @Autowired private AttendancePunchRepository attendancePunchRepository;
    @Autowired private AttendanceDeviceRepository attendanceDeviceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdDeviceIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        attendancePunchRepository.deleteAll(attendancePunchRepository.findAll().stream().filter(p -> createdUserIds.contains(p.getEmployeeUserId())).toList());
        attendanceDeviceRepository.deleteAllById(createdDeviceIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createUser(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("day-correction-test-" + UUID.randomUUID() + "@example.com");
        user.setName("Day Correction " + UUID.randomUUID());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(role);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    private void manualPunch(UserEntity employee, UserEntity manager, int hour, int minute, PunchDirection direction) {
        OffsetDateTime at = DAY.atTime(hour, minute).atZone(HOTEL_ZONE).toOffsetDateTime();
        attendanceService.recordPunch(new AttendancePunchCreateInput(employee.getId(), at, direction), manager.getId());
    }

    private AttendanceDayCorrectionInput correction(UserEntity employee, String reason, String... times) {
        return new AttendanceDayCorrectionInput(employee.getId(), DAY.toString(), List.of(times), reason);
    }

    private List<AttendancePunch> live(UserEntity employee) {
        return attendanceService.list(employee.getId(), DAY, DAY);
    }

    @Test
    void correctingATime_voidsTheOldPunchesAndKeepsThemAsHistory() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        manualPunch(employee, manager, 9, 0, PunchDirection.IN);
        manualPunch(employee, manager, 17, 0, PunchDirection.OUT);

        List<AttendancePunch> after = attendanceService.correctDay(correction(employee, "typed 17:00, was 18:00", "09:00", "18:00"), manager.getId());

        assertThat(after).hasSize(2);
        assertThat(after.get(1).getPunchAt().toLocalTime().toString()).isEqualTo("18:00");
        assertThat(after).allSatisfy(p -> assertThat(p.getCorrectionId()).isNotNull());
        assertThat(live(employee)).hasSize(2);

        List<AttendancePunch> history = attendanceService.list(employee.getId(), DAY, DAY, true);
        assertThat(history).hasSize(4);
        List<AttendancePunch> voided = history.stream().filter(p -> p.getVoidedAt() != null).toList();
        assertThat(voided).hasSize(2);
        assertThat(voided).allSatisfy(p -> {
            assertThat(p.getVoidReason()).isEqualTo("typed 17:00, was 18:00");
            assertThat(p.getVoidedByEmail()).isEqualTo(manager.getEmail());
        });
        assertThat(voided.stream().map(p -> p.getPunchAt().toLocalTime().toString())).containsExactly("09:00", "17:00");
    }

    @Test
    void voidedPunchesCountInNoReport() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        manualPunch(employee, manager, 9, 0, PunchDirection.IN);
        manualPunch(employee, manager, 17, 0, PunchDirection.OUT);

        attendanceService.correctDay(correction(employee, "left at 15:00", "09:00", "15:00"), manager.getId());

        AttendanceDaySummary summary = attendanceService.summary(employee.getId(), 2027, 7).stream().filter(d -> d.getDate().equals(DAY.toString())).findFirst().orElseThrow();
        assertThat(summary.getPunches()).hasSize(2);
        assertThat(summary.getWorkedMinutes().get()).isEqualTo(6 * 60);
        assertThat(summary.getIncomplete()).isFalse();
    }

    @Test
    void clearingTheDay_voidsEverythingAndRecordsNothing() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        manualPunch(employee, manager, 9, 0, PunchDirection.IN);

        List<AttendancePunch> after = attendanceService.correctDay(correction(employee, "wasn't here"), manager.getId());

        assertThat(after).isEmpty();
        assertThat(live(employee)).isEmpty();
        assertThat(attendanceService.list(employee.getId(), DAY, DAY, true)).hasSize(1);
    }

    @Test
    void rollingBack_isJustAnotherCorrectionWithTheEarlierTimes() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        manualPunch(employee, manager, 9, 0, PunchDirection.IN);
        manualPunch(employee, manager, 17, 0, PunchDirection.OUT);
        attendanceService.correctDay(correction(employee, "mistake", "10:00", "11:00"), manager.getId());

        attendanceService.correctDay(correction(employee, "rolled back", "09:00", "17:00"), manager.getId());

        assertThat(live(employee).stream().map(p -> p.getPunchAt().toLocalTime().toString())).containsExactly("09:00", "17:00");
        // 2 original + 2 mistaken + 2 restored - nothing was ever deleted.
        assertThat(attendanceService.list(employee.getId(), DAY, DAY, true)).hasSize(6);
    }

    @Test
    void directionsAlternateInTimeOrder_whateverTheCallerSends() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);

        List<AttendancePunch> after = attendanceService.correctDay(correction(employee, "split shift", "08:00", "12:00", "16:00", "21:00"), manager.getId());

        assertThat(after.stream().map(AttendancePunch::getDirection)).containsExactly(PunchDirection.IN, PunchDirection.OUT, PunchDirection.IN, PunchDirection.OUT);
        assertThat(after).allSatisfy(p -> assertThat(p.getSource()).isEqualTo(PunchSource.MANUAL));
    }

    @Test
    void timesAreHotelTime_notTheCallersZone() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);

        List<AttendancePunch> after = attendanceService.correctDay(correction(employee, "arrival", "09:00"), manager.getId());

        assertThat(after.get(0).getPunchAt().toString()).isEqualTo("2027-07-01T09:00+07:00");
    }

    @Test
    void aReasonIsRequired() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);

        assertThatThrownBy(() -> attendanceService.correctDay(correction(employee, "   ", "09:00"), manager.getId())).isInstanceOf(ValidationException.class);
        assertThat(live(employee)).isEmpty();
    }

    @Test
    void timesMustIncrease_andNotBeInTheFuture() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);

        assertThatThrownBy(() -> attendanceService.correctDay(correction(employee, "x", "17:00", "09:00"), manager.getId())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> attendanceService.correctDay(correction(employee, "x", "09:00", "09:00"), manager.getId())).isInstanceOf(ValidationException.class);
        AttendanceDayCorrectionInput future = new AttendanceDayCorrectionInput(employee.getId(), "2028-01-01", List.of("10:00"), "x");
        assertThatThrownBy(() -> attendanceService.correctDay(future, manager.getId())).isInstanceOf(ValidationException.class);
    }

    @Test
    void sendingTheSameTimesAgain_isRefusedRatherThanVoidingForNothing() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        manualPunch(employee, manager, 9, 0, PunchDirection.IN);

        assertThatThrownBy(() -> attendanceService.correctDay(correction(employee, "x", "09:00"), manager.getId())).isInstanceOf(ValidationException.class);
        assertThat(attendanceService.list(employee.getId(), DAY, DAY, true)).hasSize(1);
    }

    /** A scanner punch fixed by hand must not come back when the next poll re-reads the same record. */
    @Test
    void aVoidedScannerPunch_isNotReIngested() {
        UserEntity manager = createUser(Role.MANAGER);
        UserEntity employee = createUser(Role.WAITER);
        int enrollment = Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1;
        employee.setEnrollmentNumber(enrollment);
        userRepository.saveAndFlush(employee);
        AttendanceDeviceEntity device = new AttendanceDeviceEntity();
        device.setName("Test Terminal " + UUID.randomUUID());
        device.setSerial("SN-" + UUID.randomUUID());
        device.setAddress("192.168.1.99");
        device.setTimezone("Asia/Bangkok");
        createdDeviceIds.add(attendanceDeviceRepository.saveAndFlush(device).getId());
        java.time.LocalDateTime scanned = DAY.atTime(9, 0);
        attendanceService.ingestDevicePunch(device, enrollment, scanned, PunchDirection.IN);

        attendanceService.correctDay(correction(employee, "scanned for the wrong person", "10:00"), manager.getId());
        DeviceIngestResult again = attendanceService.ingestDevicePunch(device, enrollment, scanned, PunchDirection.IN);

        assertThat(again).isEqualTo(DeviceIngestResult.DUPLICATE);
        assertThat(live(employee).stream().map(p -> p.getPunchAt().toLocalTime().toString())).containsExactly("10:00");
        List<AttendancePunchEntity> all = attendancePunchRepository.findByEmployeeUserIdAndPunchAtBetweenOrderByPunchAt(employee.getId(), DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay());
        assertThat(all.stream().filter(AttendancePunchEntity::isVoided).map(AttendancePunchEntity::getSource)).containsExactly(PunchSource.SCANNER);
    }
}
