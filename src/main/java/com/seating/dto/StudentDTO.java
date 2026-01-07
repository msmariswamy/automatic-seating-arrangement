package com.seating.dto;

import lombok.*;

import java.util.Set;

/**
 * DTO for Student data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDTO {
    private String rollNo;
    private String name;
    private String department;
    private String className;
    private Set<String> subjects;
}
