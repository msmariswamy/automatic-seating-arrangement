package com.seating.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Student Entity
 */
@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_roll_no", columnList = "roll_no"),
    @Index(name = "idx_department", columnList = "department"),
    @Index(name = "idx_class", columnList = "class_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @NotBlank
    @Column(name = "roll_no", unique = true, nullable = false, length = 50)
    private String rollNo;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String department;

    @NotBlank
    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_subjects", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "subject")
    @Builder.Default
    private Set<String> subjects = new HashSet<>();

    @Column(name = "is_allocated")
    @Builder.Default
    private Boolean isAllocated = false;

    @Override
    public String toString() {
        return "Student{" +
                "rollNo='" + rollNo + '\'' +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", className='" + className + '\'' +
                ", subjects=" + subjects +
                '}';
    }
}
