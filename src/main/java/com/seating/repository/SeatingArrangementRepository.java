package com.seating.repository;

import com.seating.entity.Room;
import com.seating.entity.SeatingArrangement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for SeatingArrangement entity
 */
@Repository
public interface SeatingArrangementRepository extends JpaRepository<SeatingArrangement, Long> {

    List<SeatingArrangement> findByArrangementDate(LocalDate arrangementDate);

    List<SeatingArrangement> findByRoom(Room room);

    List<SeatingArrangement> findByRoomAndArrangementDate(Room room, LocalDate arrangementDate);

    @Query("SELECT sa FROM SeatingArrangement sa " +
           "WHERE sa.arrangementDate = :date " +
           "ORDER BY sa.room.roomNo, sa.seat.benchNo, sa.seat.position")
    List<SeatingArrangement> findByArrangementDateOrdered(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT sa.arrangementDate FROM SeatingArrangement sa ORDER BY sa.arrangementDate DESC")
    List<LocalDate> findAllArrangementDates();

    @Query("SELECT DISTINCT sa.arrangementName FROM SeatingArrangement sa WHERE sa.arrangementName IS NOT NULL")
    List<String> findAllArrangementNames();

    @Modifying
    @Query("DELETE FROM SeatingArrangement sa WHERE sa.arrangementDate = :date")
    void deleteByArrangementDate(@Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM SeatingArrangement")
    void deleteAllArrangements();
}
