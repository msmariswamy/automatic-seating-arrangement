package com.seating.controller;

import com.seating.config.ReportConfig;
import com.seating.dto.ConsolidatedReportDTO;
import com.seating.dto.DepartmentConsolidatedReportDTO;
import com.seating.dto.JuniorSupervisorReportDTO;
import com.seating.dto.MarksheetReportDTO;
import com.seating.dto.RoomReportDTO;
import com.seating.dto.SeatingFilterDTO;
import com.seating.service.ReportExcelService;
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

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SeatingArrangementService seatingService;
    private final ReportExcelService reportExcelService;
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

    @GetMapping("/reports/department-consolidated")
    @ResponseBody
    public ResponseEntity<List<DepartmentConsolidatedReportDTO>> getDepartmentConsolidatedReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<DepartmentConsolidatedReportDTO> report = seatingService.getDepartmentConsolidatedReport(date);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error fetching department consolidated report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/room/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadRoomExcel(
            @RequestParam String roomNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "") String time) {
        try {
            List<RoomReportDTO> reports = seatingService.getRoomReports(date);
            RoomReportDTO roomReport = reports.stream()
                    .filter(r -> r.getRoomNo().equals(roomNo))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Room not found in reports"));

            byte[] excelData = reportExcelService.generateRoomReportExcel(roomReport, date, time);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "room_" + roomNo + "_report.xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating room Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/consolidated/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadConsolidatedExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean showAllRollNumbers) {
        try {
            List<ConsolidatedReportDTO> report = seatingService.getConsolidatedReport(date);
            byte[] excelData = reportExcelService.generateConsolidatedReportExcel(report, date, showAllRollNumbers);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "consolidated_report_" + date + ".xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating consolidated Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/all-rooms/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAllRoomsExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "") String time) {
        try {
            List<RoomReportDTO> reports = seatingService.getRoomReports(date);
            byte[] excelData = reportExcelService.generateAllRoomsReportExcel(reports, date, time);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "all_rooms_report_" + date + ".xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating all rooms Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/department-consolidated/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadDepartmentConsolidatedExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean showAllRollNumbers) {
        try {
            List<DepartmentConsolidatedReportDTO> report = seatingService.getDepartmentConsolidatedReport(date);
            byte[] excelData = reportExcelService.generateDepartmentConsolidatedReportExcel(report, date, showAllRollNumbers);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "department_consolidated_report_" + date + ".xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating department consolidated Excel: {}", e.getMessage(), e);
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

    @GetMapping("/reports/junior-supervisor")
    @ResponseBody
    public ResponseEntity<List<JuniorSupervisorReportDTO>> getJuniorSupervisorReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<JuniorSupervisorReportDTO> reports = seatingService.getJuniorSupervisorReports(date);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error fetching junior supervisor reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/junior-supervisor/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadJuniorSupervisorExcel(
            @RequestParam String roomNo,
            @RequestParam String subject,
            @RequestParam String department,
            @RequestParam String className,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "") String time,
            @RequestParam(defaultValue = "") String sem,
            @RequestParam(defaultValue = "") String examType) {
        try {
            List<JuniorSupervisorReportDTO> reports = seatingService.getJuniorSupervisorReports(date);
            JuniorSupervisorReportDTO report = reports.stream()
                    .filter(r -> r.getRoomNo().equals(roomNo) && r.getSubject().equals(subject)
                            && r.getDepartment().equals(department) && r.getClassName().equals(className))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Report not found for room " + roomNo
                            + ", subject " + subject + ", department " + department + ", class " + className));

            byte[] excelData = reportExcelService.generateJuniorSupervisorReportExcel(report, date, time, sem, examType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            String filename = "jr_supervisor_" + roomNo + "_" + subject.replaceAll("[^a-zA-Z0-9]", "_")
                    + "_" + department.replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating Junior Supervisor Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/junior-supervisor/all/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAllJuniorSupervisorExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "") String time,
            @RequestParam(defaultValue = "") String sem,
            @RequestParam(defaultValue = "") String examType) {
        try {
            List<JuniorSupervisorReportDTO> reports = seatingService.getJuniorSupervisorReports(date);
            byte[] excelData = reportExcelService.generateAllJuniorSupervisorReportExcel(reports, date, time, sem, examType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "all_junior_supervisor_reports_" + date + ".xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating all Junior Supervisor Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/marksheet")
    @ResponseBody
    public ResponseEntity<List<MarksheetReportDTO>> getMarksheetReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<MarksheetReportDTO> reports = seatingService.getMarksheetReports(date);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error fetching marksheet reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/marksheet/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadMarksheetExcel(
            @RequestParam String subject,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "3") int blankColumns,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String className) {
        try {
            List<MarksheetReportDTO> reports = seatingService.getMarksheetReports(date);
            MarksheetReportDTO report = reports.stream()
                    .filter(r -> r.getSubject().equals(subject))
                    .filter(r -> department == null || r.getDepartment().equals(department))
                    .filter(r -> className == null || r.getClassName().equals(className))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Report not found for subject " + subject));

            byte[] excelData = reportExcelService.generateMarksheetReportExcel(report, date, blankColumns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            String filename = "marksheet_" + subject.replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating Marksheet Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/marksheet/all/excel")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAllMarksheetExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "3") int blankColumns) {
        try {
            List<MarksheetReportDTO> reports = seatingService.getMarksheetReports(date);
            byte[] excelData = reportExcelService.generateAllMarksheetReportExcel(reports, date, blankColumns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL_MEDIA_TYPE);
            headers.setContentDispositionFormData("attachment", "all_marksheets_" + date + ".xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating all Marksheet Excel: {}", e.getMessage(), e);
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

    @DeleteMapping("/all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAllArrangements() {
        Map<String, Object> response = new HashMap<>();
        try {
            seatingService.deleteAllArrangements();
            response.put("success", true);
            response.put("message", "All seating arrangements deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting all arrangements: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
