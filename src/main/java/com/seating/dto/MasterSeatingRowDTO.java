package com.seating.dto;

import lombok.*;

/**
 * DTO representing one row of the Master Seating Excel file
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterSeatingRowDTO {

    private String department;
    private String roomNo;
    private Integer noOfStudents;
    private String position; // "L" or "R"
    private String subjectName;
}
