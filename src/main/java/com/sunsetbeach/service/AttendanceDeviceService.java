package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.AttendanceDeviceMapper;
import com.sunsetbeach.model.AttendanceDevice;
import com.sunsetbeach.model.AttendanceDeviceInput;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the fingerprint terminals themselves - see {@code AttendanceDevice}'s own openapi.yaml
 * description for the pull-vs-push security posture and what {@code lastSeenAt} is for. Mirrors
 * {@code PrinterService}'s own CRUD shape (a physical networked device, unique-serial-not-unique-
 * host, deactivate-don't-delete-once-referenced) rather than inventing a new one.
 */
@Service
public class AttendanceDeviceService {

    private final AttendanceDeviceRepository attendanceDeviceRepository;
    private final AttendancePunchRepository attendancePunchRepository;
    private final AttendanceDeviceMapper attendanceDeviceMapper;
    private final AttendanceDevicePollService attendanceDevicePollService;
    private final AuditLogService auditLogService;

    public AttendanceDeviceService(
            AttendanceDeviceRepository attendanceDeviceRepository,
            AttendancePunchRepository attendancePunchRepository,
            AttendanceDeviceMapper attendanceDeviceMapper,
            AttendanceDevicePollService attendanceDevicePollService,
            AuditLogService auditLogService) {
        this.attendanceDeviceRepository = attendanceDeviceRepository;
        this.attendancePunchRepository = attendancePunchRepository;
        this.attendanceDeviceMapper = attendanceDeviceMapper;
        this.attendanceDevicePollService = attendanceDevicePollService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AttendanceDevice> list() {
        return attendanceDeviceRepository.findAll().stream().map(attendanceDeviceMapper::toDto).toList();
    }

    @Transactional
    public AttendanceDevice create(AttendanceDeviceInput input) {
        AttendanceDeviceEntity entity = new AttendanceDeviceEntity();
        attendanceDeviceMapper.applyInput(entity, input);
        AttendanceDeviceEntity saved;
        try {
            saved = attendanceDeviceRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A device with this serial is already registered");
        }
        auditLogService.record(
                AuditAction.ATTENDANCE_DEVICE_CREATED, AuditEntityType.ATTENDANCE_DEVICE, saved.getId(), "Attendance device " + saved.getName() + " created");
        return attendanceDeviceMapper.toDto(saved);
    }

    @Transactional
    public AttendanceDevice update(String id, AttendanceDeviceInput input) {
        AttendanceDeviceEntity entity = findOrThrow(id);
        String oldName = entity.getName();
        boolean oldActive = entity.isActive();
        attendanceDeviceMapper.applyInput(entity, input);
        AttendanceDeviceEntity saved;
        try {
            saved = attendanceDeviceRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A device with this serial is already registered");
        }

        StringBuilder summary = new StringBuilder("Attendance device ").append(oldName).append(" updated");
        if (!Objects.equals(oldName, saved.getName())) {
            summary.append(" (renamed to ").append(saved.getName()).append(")");
        }
        if (oldActive != saved.isActive()) {
            summary.append(saved.isActive() ? "; reactivated" : "; deactivated");
        }
        auditLogService.record(AuditAction.ATTENDANCE_DEVICE_UPDATED, AuditEntityType.ATTENDANCE_DEVICE, saved.getId(), summary.toString());

        return attendanceDeviceMapper.toDto(saved);
    }

    /**
     * The "manual re-sync" this feature's own design keeps available for a device a windowed poll
     * can no longer catch up on its own - see {@code AttendanceDevicePollService}'s own javadoc.
     * Deliberately not {@code @Transactional}: the actual work is a real TCP round trip to the
     * device, done by {@code AttendanceDevicePollService#resyncNow} in its own short transactions,
     * the same non-transactional-sweep-method shape {@code pollDevices} itself uses.
     */
    public AttendanceDevice resync(String id) {
        return attendanceDeviceMapper.toDto(attendanceDevicePollService.resyncNow(id));
    }

    @Transactional
    public void delete(String id) {
        AttendanceDeviceEntity entity = findOrThrow(id);
        if (attendancePunchRepository.existsByDeviceId(id)) {
            throw new ConflictException("This device has recorded punches and can't be deleted.");
        }
        attendanceDeviceRepository.delete(entity);
        auditLogService.record(
                AuditAction.ATTENDANCE_DEVICE_DELETED, AuditEntityType.ATTENDANCE_DEVICE, id, "Attendance device " + entity.getName() + " deleted");
    }

    private AttendanceDeviceEntity findOrThrow(String id) {
        return attendanceDeviceRepository.findById(id).orElseThrow(() -> new NotFoundException("Device not found"));
    }
}
