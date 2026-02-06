package com.seating.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for Marksheet Report - subject-wise report with all students across all rooms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarksheetReportDTO {
    private String subject;
    private String department;
    private String className;
    private int totalStudents;
    private List<StudentMarksheetEntry> students;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentMarksheetEntry {
        private int srNo;
        private String seatNo;  // Roll number (exam seat number)
        private String roomNo;  // For reference
    }
}
