package com.seating.service;

import com.seating.dto.RoomDTO;
import com.seating.entity.Room;
import com.seating.entity.Seat;
import com.seating.repository.RoomRepository;
import com.seating.repository.SeatRepository;
import com.seating.repository.SeatingArrangementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing rooms and seats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final SeatingArrangementRepository seatingArrangementRepository;
    private final ExcelService excelService;

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long getRoomCount() {
        return roomRepository.count();
    }

    @Transactional(readOnly = true)
    public long getTotalSeats() {
        return seatRepository.count();
    }

    @Transactional
    public void uploadRooms(MultipartFile file) throws Exception {
        try {
            List<RoomDTO> roomDTOs = excelService.parseRoomExcel(file);

            if (roomDTOs.isEmpty()) {
                throw new IllegalArgumentException("No valid room data found in Excel file");
            }

            int savedCount = 0;
            int skippedCount = 0;

            for (RoomDTO dto : roomDTOs) {
                if (roomRepository.existsByRoomNo(dto.getRoomNo())) {
                    log.warn("Room {} already exists, skipping", dto.getRoomNo());
                    skippedCount++;
                    continue;
                }

                Room room = Room.builder()
                        .roomNo(dto.getRoomNo())
                        .totalBenches(dto.getTotalBenches())
                        .capacity(dto.getCapacity())
                        .rCount(dto.getRCount())
                        .mCount(dto.getMCount())
                        .lCount(dto.getLCount())
                        .build();

                List<Seat> seats = generateSeats(room, dto);
                room.setSeats(seats);

                roomRepository.save(room);
                savedCount++;
            }

            log.info("Room upload completed. Saved: {}, Skipped: {}", savedCount, skippedCount);

        } catch (Exception e) {
            log.error("Error uploading rooms: {}", e.getMessage(), e);
            throw new Exception("Failed to upload rooms: " + e.getMessage());
        }
    }

    private List<Seat> generateSeats(Room room, RoomDTO dto) {
        List<Seat> seats = new ArrayList<>();
        int benchNo = 1;

        for (int i = 0; i < dto.getTotalBenches(); i++) {
            if (dto.getRCount() > 0 && i < dto.getRCount()) {
                Seat seat = Seat.builder()
                        .room(room)
                        .seatNo("R" + benchNo)
                        .position("R")
                        .benchNo(benchNo)
                        .isOccupied(false)
                        .build();
                seats.add(seat);
            }

            if (dto.getMCount() > 0 && i < dto.getMCount()) {
                Seat seat = Seat.builder()
                        .room(room)
                        .seatNo("M" + benchNo)
                        .position("M")
                        .benchNo(benchNo)
                        .isOccupied(false)
                        .build();
                seats.add(seat);
            }

            if (dto.getLCount() > 0 && i < dto.getLCount()) {
                Seat seat = Seat.builder()
                        .room(room)
                        .seatNo("L" + benchNo)
                        .position("L")
                        .benchNo(benchNo)
                        .isOccupied(false)
                        .build();
                seats.add(seat);
            }

            benchNo++;
        }

        log.debug("Generated {} seats for room {}", seats.size(), room.getRoomNo());
        return seats;
    }

    @Transactional
    public void resetSeats() {
        seatRepository.resetAllOccupiedSeats();
        log.info("All seats have been reset");
    }

    @Transactional
    public void deleteAllRooms() {
        // Delete in order to respect foreign key constraints:
        // 1. seating_arrangements (references seats and rooms)
        // 2. seats (references rooms)
        // 3. rooms (no dependencies)

        seatingArrangementRepository.deleteAll();
        log.info("All seating arrangements have been deleted");

        seatRepository.deleteAll();
        log.info("All seats have been deleted");

        roomRepository.deleteAll();
        log.info("All rooms have been deleted");
    }

    @Transactional(readOnly = true)
    public List<Seat> getAllAvailableSeats() {
        return seatRepository.findAllAvailableSeats();
    }
}
