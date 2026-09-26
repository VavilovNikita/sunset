package com.sunsetbeach.controller;

import com.sunsetbeach.api.ReportsApi;
import com.sunsetbeach.service.RevenueExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController implements ReportsApi {

    private final RevenueExportService revenueExportService;

    public ReportController(RevenueExportService revenueExportService) {
        this.revenueExportService = revenueExportService;
    }

    @Override
    public ResponseEntity<Resource> exportRevenue(String from, String to) {
        byte[] workbook = revenueExportService.export(from, to);
        String filename = "revenue-" + from + "-to-" + to + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(new ByteArrayResource(workbook));
    }
}
