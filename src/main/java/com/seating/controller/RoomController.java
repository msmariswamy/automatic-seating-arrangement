package com.seating.controller;

import com.seating.entity.Room;
import com.seating.service.ExcelService;
import com.seating.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for room management operations
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final ExcelService excelService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] excelData = excelService.generateRoomTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "room_template.xlsx");

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating room template: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadRooms(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            roomService.uploadRooms(file);

            response.put("success", true);
            response.put("message", "Rooms uploaded successfully");
            response.put("roomCount", roomService.getRoomCount());
            response.put("seatCount", roomService.getTotalSeats());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading rooms: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        try {
            List<Room> rooms = roomService.getAllRooms();
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            log.error("Error fetching rooms: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getRoomCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("roomCount", roomService.getRoomCount());
        response.put("seatCount", roomService.getTotalSeats());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllRooms() {
        Map<String, Object> response = new HashMap<>();
        try {
            roomService.deleteAllRooms();
            response.put("success", true);
            response.put("message", "All rooms deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting rooms: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
