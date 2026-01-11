package com.seating.dto;

import lombok.*;

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
    private String seatFrom;
    private String seatTo;
    private Integer totalCount;
}
