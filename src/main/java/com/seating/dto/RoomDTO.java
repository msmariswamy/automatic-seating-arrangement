package com.seating.dto;

import lombok.*;

/**
 * DTO for Room data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDTO {
    private String roomNo;
    private Integer totalBenches;
    private Integer capacity;
    private Integer rCount;
    private Integer mCount;
    private Integer lCount;
}
