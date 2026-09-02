package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.model.PrintJob;
import org.springframework.stereotype.Component;

@Component
public class PrintJobMapper {

    public PrintJob toDto(PrintJobEntity entity) {
        return new PrintJob(
                entity.getId(),
                entity.getPrinterId(),
                entity.getDocumentType(),
                entity.getSummary(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getLastError(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()))
                .dismissedAt(entity.getDismissedAt() == null ? null : TimestampFormat.toUtc(entity.getDismissedAt()))
                .dismissedByUserId(entity.getDismissedByUserId())
                .dismissNote(entity.getDismissNote());
    }
}
