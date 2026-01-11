package com.seating.controller;

import com.seating.entity.Student;
import com.seating.service.ExcelService;
import com.seating.service.StudentService;
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
 * Controller for student management operations
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final ExcelService excelService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] excelData = excelService.generateStudentTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "student_template.xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating student template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadStudents(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            studentService.uploadStudents(file);

            response.put("success", true);
            response.put("message", "Students uploaded successfully");
            response.put("count", studentService.getStudentCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading students: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        try {
            List<Student> students = studentService.getAllStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error fetching students: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getStudentCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", studentService.getStudentCount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/departments")
    public ResponseEntity<List<String>> getAllDepartments() {
        try {
            List<String> departments = studentService.getAllDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            log.error("Error fetching departments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/classes")
    public ResponseEntity<List<String>> getAllClasses() {
        try {
            List<String> classes = studentService.getAllClasses();
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            log.error("Error fetching classes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getAllSubjects() {
        try {
            List<String> subjects = studentService.getAllSubjects();
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            log.error("Error fetching subjects: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/department-subjects")
    public ResponseEntity<Map<String, List<String>>> getDepartmentSubjects() {
        try {
            Map<String, List<String>> mapping = studentService.getDepartmentSubjectMapping();
            return ResponseEntity.ok(mapping);
        } catch (Exception e) {
            log.error("Error fetching department-subject mapping: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllStudents() {
        Map<String, Object> response = new HashMap<>();
        try {
            studentService.deleteAllStudents();
            response.put("success", true);
            response.put("message", "All students deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting students: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
