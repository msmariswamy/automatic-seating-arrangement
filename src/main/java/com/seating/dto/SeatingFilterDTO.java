package com.seating.dto;

import lombok.*;

import java.util.Set;

/**
 * DTO for seating arrangement filter criteria
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatingFilterDTO {
    private Set<String> departments;
    private Set<String> classes;
    private Set<String> subjects;
    private String arrangementName;
}
