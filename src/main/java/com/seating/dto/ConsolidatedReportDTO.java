package com.seating.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for consolidated report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidatedReportDTO {
    private Long roomId;
    private String roomNo;
    private String department;
    private String rollNoFrom;
    private String rollNoTo;
    private List<String> allRollNumbers;
    private Integer totalCount;
}
