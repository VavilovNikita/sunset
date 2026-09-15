package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AttendanceDevice;
import com.sunsetbeach.model.AttendanceDeviceInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the fingerprint terminals themselves - mirrors {@code PrinterService}'s own shape
 * (unique serial, full-replacement PATCH, deactivate-don't-delete once referenced).
 */
@SpringBootTest
@Transactional
class AttendanceDeviceServiceTests extends AbstractIntegrationTest {

    @Autowired
    private AttendanceDeviceService attendanceDeviceService;

    @Autowired
    private AttendanceDeviceRepository attendanceDeviceRepository;

    @Autowired
    private AttendancePunchRepository attendancePunchRepository;

    @Autowired
    private UserRepository userRepository;

    private AttendanceDeviceInput sampleInput(String serial) {
        return new AttendanceDeviceInput("Front Desk Clock", serial, "192.168.1.50", "Asia/Bangkok");
    }

    @Test
    void create_returnsTheRegisteredDevice() {
        AttendanceDevice created = attendanceDeviceService.create(sampleInput("SN-" + UUID.randomUUID()));

        assertThat(created.getName()).isEqualTo("Front Desk Clock");
        assertThat(created.getPort()).isEqualTo(4370);
        assertThat(created.getActive()).isTrue();
        assertThat(created.getLastSeenAt().isPresent()).isFalse();
    }

    @Test
    void create_duplicateSerial_isConflict() {
        String serial = "SN-" + UUID.randomUUID();
        attendanceDeviceService.create(sampleInput(serial));

        assertThatThrownBy(() -> attendanceDeviceService.create(sampleInput(serial))).isInstanceOf(ConflictException.class);
    }

    @Test
    void update_isAFullReplacement() {
        AttendanceDevice created = attendanceDeviceService.create(sampleInput("SN-" + UUID.randomUUID()));

        AttendanceDevice updated = attendanceDeviceService.update(
                created.getId(), new AttendanceDeviceInput("Back Office Clock", created.getSerial(), "192.168.1.51", "Asia/Bangkok").port(9999).active(false));

        assertThat(updated.getName()).isEqualTo("Back Office Clock");
        assertThat(updated.getAddress()).isEqualTo("192.168.1.51");
        assertThat(updated.getPort()).isEqualTo(9999);
        assertThat(updated.getActive()).isFalse();
    }

    @Test
    void update_toAnotherDevicesSerial_isConflict() {
        AttendanceDevice first = attendanceDeviceService.create(sampleInput("SN-" + UUID.randomUUID()));
        AttendanceDevice second = attendanceDeviceService.create(sampleInput("SN-" + UUID.randomUUID()));

        assertThatThrownBy(() -> attendanceDeviceService.update(second.getId(), sampleInput(first.getSerial())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_unknownId_isNotFound() {
        assertThatThrownBy(() -> attendanceDeviceService.update("does-not-exist", sampleInput("SN-" + UUID.randomUUID())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_withNoPunches_succeeds() {
        AttendanceDevice created = attendanceDeviceService.create(sampleInput("SN-" + UUID.randomUUID()));

        attendanceDeviceService.delete(created.getId());

        assertThat(attendanceDeviceRepository.findById(created.getId())).isEmpty();
    }

    /** Same "history is immutable" precedent as DELETE /printers/{id} - deactivate instead. */
    @Test
    void delete_withExistingPunches_isRejected() {
        AttendanceDevice created = attendanceDeviceService.create(sampleInput("SN-" + UUID.randomUUID()));
        UserEntity employee = new UserEntity();
        employee.setName("Device Delete Guard Employee " + UUID.randomUUID());
        employee.setRole(Role.WAITER);
        employee.setActive(true);
        employee.setEnrollmentNumber(Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1);
        employee = userRepository.saveAndFlush(employee);

        AttendancePunchEntity punch = new AttendancePunchEntity();
        punch.setEmployeeUserId(employee.getId());
        punch.setPunchAt(LocalDateTime.of(2027, 6, 1, 9, 0));
        punch.setDirection(PunchDirection.IN);
        punch.setSource(PunchSource.SCANNER);
        punch.setDeviceId(created.getId());
        punch.setEnrollmentNumber(employee.getEnrollmentNumber());
        attendancePunchRepository.saveAndFlush(punch);

        assertThatThrownBy(() -> attendanceDeviceService.delete(created.getId())).isInstanceOf(ConflictException.class);
    }
}
