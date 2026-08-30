package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.model.AuditLogEntry;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogEntry toDto(AuditLogEntity entity) {
        return new AuditLogEntry(
                        entity.getId(),
                        entity.getActorUserId(),
                        entity.getActorEmail(),
                        entity.getActorRole(),
                        entity.getAction(),
                        entity.getEntityType(),
                        entity.getSummary(),
                        TimestampFormat.toUtc(entity.getCreatedAt()))
                .entityId(entity.getEntityId());
    }
}
