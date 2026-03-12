package com.seating.controller;

import com.seating.dto.MasterSeatingRowDTO;
import com.seating.service.ExcelService;
import com.seating.service.MasterSeatingStore;
import com.seating.service.SeatingArrangementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Master Seating operations
 */
@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
@Slf4j
public class MasterSeatingController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelService excelService;
    private final MasterSeatingStore masterSeatingStore;
    private final SeatingArrangementService seatingService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadMasterTemplate() {
        try {
            byte[] data = excelService.generateMasterTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "master_seating_template.xlsx");
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating master template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadMaster(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<MasterSeatingRowDTO> rows = excelService.parseMasterExcel(file);
            if (rows.isEmpty()) {
                response.put("success", false);
                response.put("message", "No valid rows found in the master file. " +
                        "Check that 'Seated in L or R' is exactly 'L' or 'R' and all fields are filled.");
                return ResponseEntity.badRequest().body(response);
            }
            masterSeatingStore.setRows(rows);
            response.put("success", true);
            response.put("message", rows.size() + " rows loaded from master file");
            response.put("rowCount", rows.size());
            response.put("rows", rows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading master seating file: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMasterStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("hasData", masterSeatingStore.hasData());
        response.put("rowCount", masterSeatingStore.getRowCount());
        response.put("rows", masterSeatingStore.getRows());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateMasterSeating(
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!masterSeatingStore.hasData()) {
                response.put("success", false);
                response.put("message", "No master seating data found. Please upload the master file first.");
                return ResponseEntity.badRequest().body(response);
            }
            String arrangementName = (body != null) ? body.getOrDefault("arrangementName", "") : "";
            Map<String, Object> result = seatingService.generateMasterSeatingArrangement(
                    masterSeatingStore.getRows(), arrangementName);
            response.put("success", true);
            response.putAll(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating master seating: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearMasterSeating() {
        Map<String, Object> response = new HashMap<>();
        masterSeatingStore.clear();
        response.put("success", true);
        response.put("message", "Master seating data cleared successfully");
        return ResponseEntity.ok(response);
    }
}
