package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.AttendanceDeviceMapper;
import com.sunsetbeach.model.AttendanceDevice;
import com.sunsetbeach.model.AttendanceDeviceInput;
import com.sunsetbeach.repository.AttendanceDeviceRepository;
import com.sunsetbeach.repository.AttendancePunchRepository;
import java.util.List;
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

    public AttendanceDeviceService(
            AttendanceDeviceRepository attendanceDeviceRepository,
            AttendancePunchRepository attendancePunchRepository,
            AttendanceDeviceMapper attendanceDeviceMapper) {
        this.attendanceDeviceRepository = attendanceDeviceRepository;
        this.attendancePunchRepository = attendancePunchRepository;
        this.attendanceDeviceMapper = attendanceDeviceMapper;
    }

    @Transactional(readOnly = true)
    public List<AttendanceDevice> list() {
        return attendanceDeviceRepository.findAll().stream().map(attendanceDeviceMapper::toDto).toList();
    }

    @Transactional
    public AttendanceDevice create(AttendanceDeviceInput input) {
        AttendanceDeviceEntity entity = new AttendanceDeviceEntity();
        attendanceDeviceMapper.applyInput(entity, input);
        try {
            return attendanceDeviceMapper.toDto(attendanceDeviceRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A device with this serial is already registered");
        }
    }

    @Transactional
    public AttendanceDevice update(String id, AttendanceDeviceInput input) {
        AttendanceDeviceEntity entity = findOrThrow(id);
        attendanceDeviceMapper.applyInput(entity, input);
        try {
            return attendanceDeviceMapper.toDto(attendanceDeviceRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A device with this serial is already registered");
        }
    }

    @Transactional
    public void delete(String id) {
        AttendanceDeviceEntity entity = findOrThrow(id);
        if (attendancePunchRepository.existsByDeviceId(id)) {
            throw new ConflictException("This device has recorded punches and can't be deleted.");
        }
        attendanceDeviceRepository.delete(entity);
    }

    private AttendanceDeviceEntity findOrThrow(String id) {
        return attendanceDeviceRepository.findById(id).orElseThrow(() -> new NotFoundException("Device not found"));
    }
}
