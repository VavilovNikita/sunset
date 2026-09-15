package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.model.AttendanceDevice;
import com.sunsetbeach.model.AttendanceDeviceInput;
import org.springframework.stereotype.Component;

@Component
public class AttendanceDeviceMapper {

    public AttendanceDevice toDto(AttendanceDeviceEntity entity) {
        AttendanceDevice dto = new AttendanceDevice(
                entity.getId(), entity.getName(), entity.getSerial(), entity.getAddress(), entity.getPort(), entity.getTimezone(), entity.isActive(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
        if (entity.getLastSeenAt() != null) {
            dto.lastSeenAt(TimestampFormat.toUtc(entity.getLastSeenAt()));
        }
        return dto;
    }

    /** AttendanceDeviceInput is a full replacement on both create and update - applies every field. */
    public void applyInput(AttendanceDeviceEntity entity, AttendanceDeviceInput input) {
        entity.setName(input.getName().trim());
        entity.setSerial(input.getSerial().trim());
        entity.setAddress(input.getAddress().trim());
        entity.setPort(input.getPort() != null ? input.getPort() : 4370);
        entity.setTimezone(input.getTimezone().trim());
        entity.setActive(input.getActive() != null ? input.getActive() : true);
    }
}
