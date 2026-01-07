package com.seating.repository;

import com.seating.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Room entity
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNo(String roomNo);

    boolean existsByRoomNo(String roomNo);

    @Modifying
    @Query("DELETE FROM Room")
    void deleteAllRooms();
}
