package com.seating.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Seat Entity representing individual seats in a room
 */
@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_room_seat", columnList = "room_id, seat_no"),
    @Index(name = "idx_bench_position", columnList = "bench_no, position")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotBlank
    @Column(name = "seat_no", nullable = false, length = 10)
    private String seatNo;

    @NotBlank
    @Column(nullable = false, length = 1)
    private String position;

    @Column(name = "bench_no", nullable = false)
    private Integer benchNo;

    @Column(name = "is_occupied")
    @Builder.Default
    private Boolean isOccupied = false;

    @Override
    public String toString() {
        return "Seat{" +
                "seatNo='" + seatNo + '\'' +
                ", position='" + position + '\'' +
                ", benchNo=" + benchNo +
                ", isOccupied=" + isOccupied +
                '}';
    }
}
