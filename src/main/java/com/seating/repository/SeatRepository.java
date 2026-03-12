package com.seating.repository;

import com.seating.entity.Room;
import com.seating.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Seat entity
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoom(Room room);

    List<Seat> findByRoomAndIsOccupied(Room room, Boolean isOccupied);

    @Query("SELECT s FROM Seat s WHERE s.room.id = :roomId AND s.isOccupied = false ORDER BY s.benchNo, s.position")
    List<Seat> findAvailableSeatsByRoom(@Param("roomId") Long roomId);

    @Query("SELECT s FROM Seat s WHERE s.isOccupied = false ORDER BY s.room.id, s.benchNo, s.position")
    List<Seat> findAllAvailableSeats();

    @Query("SELECT s FROM Seat s WHERE s.room = :room AND s.position = :position " +
           "AND s.isOccupied = false ORDER BY s.benchNo")
    List<Seat> findAvailableSeatsByRoomAndPosition(@Param("room") Room room,
                                                   @Param("position") String position);

    @Query("SELECT s FROM Seat s WHERE s.position = 'M' AND s.isOccupied = false " +
           "ORDER BY s.room.id, s.benchNo")
    List<Seat> findAvailableMSeats();

    @Modifying
    @Query("UPDATE Seat s SET s.isOccupied = false")
    void resetAllOccupiedSeats();
}
