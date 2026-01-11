package com.seating.dto;

import lombok.*;

/**
 * DTO for seat allocation in reports
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAllocationDTO {
    private String seatNo;
    private Integer benchNo;
    private String rollNo;
    private String studentName;
    private String department;
    private String subject;
}
