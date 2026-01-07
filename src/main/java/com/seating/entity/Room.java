package com.seating.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Room Entity
 */
@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_room_no", columnList = "room_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @NotBlank
    @Column(name = "room_no", unique = true, nullable = false, length = 50)
    private String roomNo;

    @Min(1)
    @Column(name = "total_benches", nullable = false)
    private Integer totalBenches;

    @Min(1)
    @Column(nullable = false)
    private Integer capacity;

    @Min(0)
    @Column(name = "r_count", nullable = false)
    private Integer rCount;

    @Min(0)
    @Column(name = "m_count", nullable = false)
    private Integer mCount;

    @Min(0)
    @Column(name = "l_count", nullable = false)
    private Integer lCount;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

    @Override
    public String toString() {
        return "Room{" +
                "roomNo='" + roomNo + '\'' +
                ", totalBenches=" + totalBenches +
                ", capacity=" + capacity +
                ", rCount=" + rCount +
                ", mCount=" + mCount +
                ", lCount=" + lCount +
                '}';
    }
}
