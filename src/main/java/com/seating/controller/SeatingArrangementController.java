package com.seating.controller;

import com.seating.config.ReportConfig;
import com.seating.dto.ConsolidatedReportDTO;
import com.seating.dto.RoomReportDTO;
import com.seating.dto.SeatingFilterDTO;
import com.seating.service.PdfService;
import com.seating.service.SeatingArrangementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for seating arrangement operations
 */
@Controller
@RequestMapping("/api/seating")
@RequiredArgsConstructor
@Slf4j
public class SeatingArrangementController {

    private final SeatingArrangementService seatingService;
    private final PdfService pdfService;
    private final ReportConfig reportConfig;

    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateSeatingArrangement(@RequestBody SeatingFilterDTO filter) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> result = seatingService.generateSeatingArrangement(filter);
            response.put("success", true);
            response.putAll(result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating seating arrangement: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/reports")
    public String showReportsPage(Model model) {
        List<LocalDate> dates = seatingService.getAllArrangementDates();
        model.addAttribute("dates", dates);
        model.addAttribute("reportHeaderLine1", reportConfig.getLine1());
        model.addAttribute("reportHeaderLine2", reportConfig.getLine2());
        return "reports";
    }

    @GetMapping("/reports/consolidated")
    @ResponseBody
    public ResponseEntity<List<ConsolidatedReportDTO>> getConsolidatedReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<ConsolidatedReportDTO> report = seatingService.getConsolidatedReport(date);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error fetching consolidated report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/rooms")
    @ResponseBody
    public ResponseEntity<List<RoomReportDTO>> getRoomReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<RoomReportDTO> reports = seatingService.getRoomReports(date);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error fetching room reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/room/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> downloadRoomPdf(
            @RequestParam String roomNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") String fontSize) {
        try {
            List<RoomReportDTO> reports = seatingService.getRoomReports(date);
            RoomReportDTO roomReport = reports.stream()
                    .filter(r -> r.getRoomNo().equals(roomNo))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Room not found in reports"));

            byte[] pdfData = pdfService.generateRoomReportPdf(roomReport, date, fontSize);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "room_" + roomNo + "_report.pdf");

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/consolidated/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> downloadConsolidatedPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") String fontSize) {
        try {
            List<ConsolidatedReportDTO> report = seatingService.getConsolidatedReport(date);
            byte[] pdfData = pdfService.generateConsolidatedReportPdf(report, date, fontSize);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "consolidated_report_" + date + ".pdf");

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating consolidated PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/all-rooms/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAllRoomsPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") String fontSize) {
        try {
            List<RoomReportDTO> reports = seatingService.getRoomReports(date);

            byte[] pdfData = pdfService.generateMergedRoomReportsPdf(reports, date, fontSize);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "all_rooms_report_" + date + ".pdf");

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating merged PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dates")
    @ResponseBody
    public ResponseEntity<List<LocalDate>> getAllArrangementDates() {
        try {
            List<LocalDate> dates = seatingService.getAllArrangementDates();
            return ResponseEntity.ok(dates);
        } catch (Exception e) {
            log.error("Error fetching arrangement dates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteArrangement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Object> response = new HashMap<>();
        try {
            seatingService.deleteArrangement(date);
            response.put("success", true);
            response.put("message", "Arrangement deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting arrangement: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
