package com.seating.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for Consolidated Room Report
 * Groups departments with their rooms and seat number ranges
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidatedRoomReportDTO {
    private Long roomId;
    private String department;
    private String className;
    private String roomNo;
    private List<String> fromSeatNumbers; // Comma-separated list of starting seat numbers
    private String toSeatNumber; // Last seat number in that room for that department
    private Integer totalCount; // Total count of seat numbers
    private List<String> allSeatNumbers; // Full list of all seat numbers (for display when checkbox unchecked)
}