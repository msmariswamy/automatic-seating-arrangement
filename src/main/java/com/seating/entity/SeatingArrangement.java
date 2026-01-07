package com.seating.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Seating Arrangement Entity representing the mapping between students and seats
 */
@Entity
@Table(name = "seating_arrangements", indexes = {
    @Index(name = "idx_arrangement_date", columnList = "arrangement_date"),
    @Index(name = "idx_room_arrangement", columnList = "room_id, arrangement_date"),
    @Index(name = "idx_student_arrangement", columnList = "student_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatingArrangement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(name = "arrangement_date", nullable = false)
    private LocalDate arrangementDate;

    @Column(name = "arrangement_name", length = 100)
    private String arrangementName;

    @Override
    public String toString() {
        return "SeatingArrangement{" +
                "subject='" + subject + '\'' +
                ", arrangementDate=" + arrangementDate +
                ", arrangementName='" + arrangementName + '\'' +
                '}';
    }
}
