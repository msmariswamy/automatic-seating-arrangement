package com.seating.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for Junior Supervisor Report - subject-wise room-wise report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JuniorSupervisorReportDTO {
    private Long roomId;
    private String roomNo;
    private String department;
    private String className;
    private String subject;
    private int totalStudents;
    private List<StudentEntry> students;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentEntry {
        private int srNo;
        private String seatNo;
        private String rollNo;
    }
}
