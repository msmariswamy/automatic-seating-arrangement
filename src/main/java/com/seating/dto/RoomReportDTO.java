package com.seating.dto;

import lombok.*;

import java.util.List;
import java.util.Set;

/**
 * DTO for individual room report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomReportDTO {
    private String roomNo;
    private Set<String> departments;
    private Set<String> subjects;
    private List<SeatAllocationDTO> rightSeats;
    private List<SeatAllocationDTO> middleSeats;
    private List<SeatAllocationDTO> leftSeats;
}
