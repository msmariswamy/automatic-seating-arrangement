package com.seating.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for department-consolidated report grouped by department, class, and room.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentConsolidatedReportDTO {
    private String department;
    private String className;
    private Long roomId;
    private String roomNo;
    private String fromSeat;       // First seat number
    private String toSeat;         // Last seat number
    private List<String> allRollNumbers;
    private Integer totalCount;    // Total number of seats
}