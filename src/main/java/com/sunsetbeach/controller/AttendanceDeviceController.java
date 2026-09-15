package com.sunsetbeach.controller;

import com.sunsetbeach.api.AttendanceDevicesApi;
import com.sunsetbeach.model.AttendanceDevice;
import com.sunsetbeach.model.AttendanceDeviceInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.service.AttendanceDeviceService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttendanceDeviceController implements AttendanceDevicesApi {

    private final AttendanceDeviceService attendanceDeviceService;

    public AttendanceDeviceController(AttendanceDeviceService attendanceDeviceService) {
        this.attendanceDeviceService = attendanceDeviceService;
    }

    @Override
    public ResponseEntity<List<AttendanceDevice>> listAttendanceDevices() {
        return ResponseEntity.ok(attendanceDeviceService.list());
    }

    @Override
    public ResponseEntity<AttendanceDevice> createAttendanceDevice(AttendanceDeviceInput attendanceDeviceInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceDeviceService.create(attendanceDeviceInput));
    }

    @Override
    public ResponseEntity<AttendanceDevice> updateAttendanceDevice(String id, AttendanceDeviceInput attendanceDeviceInput) {
        return ResponseEntity.ok(attendanceDeviceService.update(id, attendanceDeviceInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteAttendanceDevice(String id) {
        attendanceDeviceService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }

    @Override
    public ResponseEntity<AttendanceDevice> resyncAttendanceDevice(String id) {
        return ResponseEntity.ok(attendanceDeviceService.resync(id));
    }
}
