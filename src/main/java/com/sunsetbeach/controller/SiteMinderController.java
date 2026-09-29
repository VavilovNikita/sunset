package com.sunsetbeach.controller;

import com.sunsetbeach.api.SiteMinderApi;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.SiteMinderImportResult;
import com.sunsetbeach.model.SiteMinderReservationInput;
import com.sunsetbeach.model.SiteMinderRoomTypeMapping;
import com.sunsetbeach.model.SiteMinderRoomTypeMappingInput;
import com.sunsetbeach.service.SiteMinderImportService;
import com.sunsetbeach.service.SiteMinderRoomTypeMappingService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteMinderController implements SiteMinderApi {

    private final SiteMinderImportService importService;
    private final SiteMinderRoomTypeMappingService mappingService;

    public SiteMinderController(SiteMinderImportService importService, SiteMinderRoomTypeMappingService mappingService) {
        this.importService = importService;
        this.mappingService = mappingService;
    }

    @Override
    public ResponseEntity<SiteMinderImportResult> importSiteMinderReservation(SiteMinderReservationInput siteMinderReservationInput) {
        return ResponseEntity.ok(importService.importReservation(siteMinderReservationInput));
    }

    @Override
    public ResponseEntity<List<SiteMinderRoomTypeMapping>> listSiteMinderRoomTypeMappings() {
        return ResponseEntity.ok(mappingService.list());
    }

    @Override
    public ResponseEntity<SiteMinderRoomTypeMapping> createSiteMinderRoomTypeMapping(SiteMinderRoomTypeMappingInput siteMinderRoomTypeMappingInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mappingService.create(siteMinderRoomTypeMappingInput));
    }

    @Override
    public ResponseEntity<SiteMinderRoomTypeMapping> updateSiteMinderRoomTypeMapping(String id, SiteMinderRoomTypeMappingInput siteMinderRoomTypeMappingInput) {
        return ResponseEntity.ok(mappingService.update(id, siteMinderRoomTypeMappingInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteSiteMinderRoomTypeMapping(String id) {
        mappingService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }
}
