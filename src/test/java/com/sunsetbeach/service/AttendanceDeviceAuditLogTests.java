package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.model.AttendanceDevice;
import com.sunsetbeach.model.AttendanceDeviceInput;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - see {@link BookingAuditLogTests}'s
 * javadoc for why (AuditLogService.record commits independently of this test's transaction).
 */
@SpringBootTest
class AttendanceDeviceAuditLogTests extends AbstractIntegrationTest {

    @Autowired
    private AttendanceDeviceService attendanceDeviceService;

    @Autowired
    private AttendanceDeviceRepository attendanceDeviceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdDeviceIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", "audit-admin-test@example.com", Role.ADMIN);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String deviceId : createdDeviceIds) {
            auditLogRepository.deleteAll(entriesFor(deviceId));
            attendanceDeviceRepository.findById(deviceId).ifPresent(attendanceDeviceRepository::delete);
        }
    }

    private List<AuditLogEntity> entriesFor(String deviceId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.ATTENDANCE_DEVICE && deviceId.equals(e.getEntityId()))
                .toList();
    }

    @Test
    void createUpdateDeleteDevice_writeExpectedEntries() {
        AttendanceDevice created = attendanceDeviceService.create(
                new AttendanceDeviceInput("Audit Device " + UUID.randomUUID(), "SN-" + UUID.randomUUID(), "192.168.1.60", "Asia/Bangkok"));
        createdDeviceIds.add(created.getId());

        List<AuditLogEntity> createdEntries =
                entriesFor(created.getId()).stream().filter(e -> e.getAction() == AuditAction.ATTENDANCE_DEVICE_CREATED).toList();
        assertThat(createdEntries).hasSize(1);
        assertThat(createdEntries.get(0).getSummary()).contains(created.getName());

        String newName = "Renamed Device " + UUID.randomUUID();
        attendanceDeviceService.update(created.getId(), new AttendanceDeviceInput(newName, created.getSerial(), created.getAddress(), "Asia/Bangkok"));

        List<AuditLogEntity> updatedEntries =
                entriesFor(created.getId()).stream().filter(e -> e.getAction() == AuditAction.ATTENDANCE_DEVICE_UPDATED).toList();
        assertThat(updatedEntries).hasSize(1);
        assertThat(updatedEntries.get(0).getSummary()).contains("renamed").contains(newName);

        String deviceId = created.getId();
        attendanceDeviceService.delete(deviceId);
        createdDeviceIds.remove(deviceId);

        List<AuditLogEntity> deletedEntries =
                entriesFor(deviceId).stream().filter(e -> e.getAction() == AuditAction.ATTENDANCE_DEVICE_DELETED).toList();
        assertThat(deletedEntries).hasSize(1);
        auditLogRepository.deleteAll(entriesFor(deviceId));
    }
}
