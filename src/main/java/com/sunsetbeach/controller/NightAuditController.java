package com.sunsetbeach.controller;

import com.sunsetbeach.api.NightAuditApi;
import com.sunsetbeach.model.NightAudit;
import com.sunsetbeach.model.NightAuditCloseInput;
import com.sunsetbeach.model.NightAuditClosure;
import com.sunsetbeach.service.NightAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NightAuditController implements NightAuditApi {

    private final NightAuditService nightAuditService;

    public NightAuditController(NightAuditService nightAuditService) {
        this.nightAuditService = nightAuditService;
    }

    @Override
    public ResponseEntity<NightAudit> getNightAudit(String date) {
        return ResponseEntity.ok(nightAuditService.get(date));
    }

    @Override
    public ResponseEntity<NightAuditClosure> closeNightAudit(NightAuditCloseInput nightAuditCloseInput) {
        return ResponseEntity.ok(nightAuditService.close(nightAuditCloseInput));
    }
}
