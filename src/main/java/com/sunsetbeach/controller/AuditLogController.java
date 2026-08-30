package com.sunsetbeach.controller;

import com.sunsetbeach.api.AuditLogApi;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.AuditLogPage;
import com.sunsetbeach.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController implements AuditLogApi {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public ResponseEntity<AuditLogPage> listAuditLog(
            String actorEmail, AuditAction action, AuditEntityType entityType, String entityId, String from, String to, Integer page, Integer pageSize) {
        return ResponseEntity.ok(auditLogService.search(actorEmail, action, entityType, entityId, from, to, page, pageSize));
    }
}
