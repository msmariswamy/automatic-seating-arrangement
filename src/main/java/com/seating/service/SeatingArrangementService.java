package com.seating.service;

import com.seating.dto.*;
import com.seating.entity.*;
import com.seating.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing seating arrangements with constraint checking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatingArrangementService {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final SeatingArrangementRepository arrangementRepository;

    @Transactional
    public Map<String, Object> generateSeatingArrangement(SeatingFilterDTO filter) throws Exception {
        try {
            if (filter.getDepartments() == null || filter.getDepartments().isEmpty() ||
                filter.getClasses() == null || filter.getClasses().isEmpty() ||
                filter.getSubjects() == null || filter.getSubjects().isEmpty()) {
                throw new IllegalArgumentException("Please select departments, classes, and subjects");
            }

            List<Student> students = studentRepository.findByDepartmentsAndClassesAndSubjects(
                    filter.getDepartments(), filter.getClasses(), filter.getSubjects());

            if (students.isEmpty()) {
                throw new IllegalArgumentException("No students found matching the selected criteria");
            }

            List<Room> rooms = roomRepository.findAll();
            if (rooms.isEmpty()) {
                throw new IllegalArgumentException("No rooms available. Please add rooms first.");
            }

            resetPreviousArrangement();

            List<SeatingArrangement> arrangements = allocateSeats(students, rooms, filter);

            if (arrangements.isEmpty()) {
                throw new Exception("Unable to generate seating arrangement. Please check room capacity.");
            }

            arrangementRepository.saveAll(arrangements);

            Map<String, Object> result = new HashMap<>();
            result.put("totalStudents", arrangements.size());
            result.put("roomsUsed", arrangements.stream()
                    .map(a -> a.getRoom().getRoomNo())
                    .distinct()
                    .count());
            result.put("arrangementDate", LocalDate.now());
            result.put("message", "Seating arrangement generated successfully");

            log.info("Generated seating arrangement for {} students across {} rooms",
                    arrangements.size(), result.get("roomsUsed"));

            return result;

        } catch (Exception e) {
            log.error("Error generating seating arrangement: {}", e.getMessage(), e);
            throw e;
        }
    }

    private List<SeatingArrangement> allocateSeats(List<Student> students, List<Room> rooms,
                                                   SeatingFilterDTO filter) {
        List<SeatingArrangement> arrangements = new ArrayList<>();
        List<Seat> allSeats = new ArrayList<>();

        for (Room room : rooms) {
            List<Seat> roomSeats = seatRepository.findAvailableSeatsByRoom(room.getId());
            allSeats.addAll(roomSeats);
        }

        if (allSeats.isEmpty()) {
            log.error("No available seats found in any room");
            return arrangements;
        }

        Map<Integer, List<Seat>> seatsByBench = groupSeatsByBench(allSeats);
        Map<String, Queue<Student>> studentsBySubject = groupStudentsBySubject(students, filter.getSubjects());

        for (Map.Entry<Integer, List<Seat>> benchEntry : seatsByBench.entrySet()) {
            List<Seat> benchSeats = benchEntry.getValue();

            if (benchSeats.size() < 3) {
                continue;
            }

            Seat rightSeat = benchSeats.stream().filter(s -> "R".equals(s.getPosition())).findFirst().orElse(null);
            Seat middleSeat = benchSeats.stream().filter(s -> "M".equals(s.getPosition())).findFirst().orElse(null);
            Seat leftSeat = benchSeats.stream().filter(s -> "L".equals(s.getPosition())).findFirst().orElse(null);

            Set<String> usedSubjects = new HashSet<>();

            if (rightSeat != null) {
                Student student = findStudentWithDifferentSubject(studentsBySubject, usedSubjects);
                if (student != null) {
                    String subject = getStudentSubject(student, filter.getSubjects());
                    if (subject != null) {
                        arrangements.add(createArrangement(student, rightSeat, subject, filter));
                        usedSubjects.add(subject);
                        rightSeat.setIsOccupied(true);
                        student.setIsAllocated(true);
                    }
                }
            }

            if (middleSeat != null) {
                Student student = findStudentWithDifferentSubject(studentsBySubject, usedSubjects);
                if (student != null) {
                    String subject = getStudentSubject(student, filter.getSubjects());
                    if (subject != null) {
                        arrangements.add(createArrangement(student, middleSeat, subject, filter));
                        usedSubjects.add(subject);
                        middleSeat.setIsOccupied(true);
                        student.setIsAllocated(true);
                    }
                }
            }

            if (leftSeat != null) {
                Student student = findStudentWithDifferentSubject(studentsBySubject, usedSubjects);
                if (student != null) {
                    String subject = getStudentSubject(student, filter.getSubjects());
                    if (subject != null) {
                        arrangements.add(createArrangement(student, leftSeat, subject, filter));
                        usedSubjects.add(subject);
                        leftSeat.setIsOccupied(true);
                        student.setIsAllocated(true);
                    }
                }
            }
        }

        return arrangements;
    }

    private Map<Integer, List<Seat>> groupSeatsByBench(List<Seat> seats) {
        Map<Integer, List<Seat>> seatsByBench = new HashMap<>();

        for (Seat seat : seats) {
            int benchKey = seat.getRoom().getId().intValue() * 10000 + seat.getBenchNo();
            seatsByBench.computeIfAbsent(benchKey, k -> new ArrayList<>()).add(seat);
        }

        return seatsByBench;
    }

    private Map<String, Queue<Student>> groupStudentsBySubject(List<Student> students, Set<String> filterSubjects) {
        Map<String, Queue<Student>> studentsBySubject = new HashMap<>();

        for (Student student : students) {
            for (String subject : student.getSubjects()) {
                if (filterSubjects.contains(subject)) {
                    studentsBySubject.computeIfAbsent(subject, k -> new LinkedList<>()).add(student);
                    break;
                }
            }
        }

        return studentsBySubject;
    }

    private Student findStudentWithDifferentSubject(Map<String, Queue<Student>> studentsBySubject,
                                                    Set<String> usedSubjects) {
        for (Map.Entry<String, Queue<Student>> entry : studentsBySubject.entrySet()) {
            String subject = entry.getKey();
            Queue<Student> queue = entry.getValue();

            if (!usedSubjects.contains(subject)) {
                while (!queue.isEmpty()) {
                    Student student = queue.poll();
                    if (!student.getIsAllocated()) {
                        return student;
                    }
                }
            }
        }

        for (Queue<Student> queue : studentsBySubject.values()) {
            while (!queue.isEmpty()) {
                Student student = queue.poll();
                if (!student.getIsAllocated()) {
                    return student;
                }
            }
        }

        return null;
    }

    private String getStudentSubject(Student student, Set<String> filterSubjects) {
        for (String subject : student.getSubjects()) {
            if (filterSubjects.contains(subject)) {
                return subject;
            }
        }
        return student.getSubjects().iterator().next();
    }

    private SeatingArrangement createArrangement(Student student, Seat seat, String subject,
                                                 SeatingFilterDTO filter) {
        return SeatingArrangement.builder()
                .student(student)
                .room(seat.getRoom())
                .seat(seat)
                .subject(subject)
                .arrangementDate(LocalDate.now())
                .arrangementName(filter.getArrangementName())
                .build();
    }

    private void resetPreviousArrangement() {
        studentRepository.resetAllAllocations();
        seatRepository.resetAllOccupiedSeats();
    }

    @Transactional(readOnly = true)
    public List<ConsolidatedReportDTO> getConsolidatedReport(LocalDate date) {
        List<SeatingArrangement> arrangements = arrangementRepository.findByArrangementDateOrdered(date);

        Map<String, Map<String, List<SeatingArrangement>>> groupedData = arrangements.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getRoom().getRoomNo(),
                        Collectors.groupingBy(a -> a.getStudent().getDepartment())
                ));

        List<ConsolidatedReportDTO> report = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<SeatingArrangement>>> roomEntry : groupedData.entrySet()) {
            String roomNo = roomEntry.getKey();

            for (Map.Entry<String, List<SeatingArrangement>> deptEntry : roomEntry.getValue().entrySet()) {
                String department = deptEntry.getKey();
                List<SeatingArrangement> deptArrangements = deptEntry.getValue();

                deptArrangements.sort(Comparator.comparing(a -> a.getSeat().getSeatNo()));

                String seatFrom = deptArrangements.get(0).getSeat().getSeatNo();
                String seatTo = deptArrangements.get(deptArrangements.size() - 1).getSeat().getSeatNo();

                ConsolidatedReportDTO dto = ConsolidatedReportDTO.builder()
                        .roomNo(roomNo)
                        .department(department)
                        .seatFrom(seatFrom)
                        .seatTo(seatTo)
                        .totalCount(deptArrangements.size())
                        .build();

                report.add(dto);
            }
        }

        report.sort(Comparator.comparing(ConsolidatedReportDTO::getRoomNo)
                .thenComparing(ConsolidatedReportDTO::getDepartment));

        return report;
    }

    @Transactional(readOnly = true)
    public List<RoomReportDTO> getRoomReports(LocalDate date) {
        List<SeatingArrangement> arrangements = arrangementRepository.findByArrangementDateOrdered(date);

        Map<String, List<SeatingArrangement>> byRoom = arrangements.stream()
                .collect(Collectors.groupingBy(a -> a.getRoom().getRoomNo()));

        List<RoomReportDTO> reports = new ArrayList<>();

        for (Map.Entry<String, List<SeatingArrangement>> entry : byRoom.entrySet()) {
            String roomNo = entry.getKey();
            List<SeatingArrangement> roomArrangements = entry.getValue();

            Set<String> departments = roomArrangements.stream()
                    .map(a -> a.getStudent().getDepartment())
                    .collect(Collectors.toSet());

            Set<String> subjects = roomArrangements.stream()
                    .map(SeatingArrangement::getSubject)
                    .collect(Collectors.toSet());

            List<SeatAllocationDTO> rightSeats = new ArrayList<>();
            List<SeatAllocationDTO> middleSeats = new ArrayList<>();
            List<SeatAllocationDTO> leftSeats = new ArrayList<>();

            for (SeatingArrangement arr : roomArrangements) {
                SeatAllocationDTO allocation = SeatAllocationDTO.builder()
                        .seatNo(arr.getSeat().getSeatNo())
                        .rollNo(arr.getStudent().getRollNo())
                        .studentName(arr.getStudent().getName())
                        .department(arr.getStudent().getDepartment())
                        .subject(arr.getSubject())
                        .build();

                switch (arr.getSeat().getPosition()) {
                    case "R" -> rightSeats.add(allocation);
                    case "M" -> middleSeats.add(allocation);
                    case "L" -> leftSeats.add(allocation);
                }
            }

            rightSeats.sort(Comparator.comparing(SeatAllocationDTO::getSeatNo));
            middleSeats.sort(Comparator.comparing(SeatAllocationDTO::getSeatNo));
            leftSeats.sort(Comparator.comparing(SeatAllocationDTO::getSeatNo));

            RoomReportDTO report = RoomReportDTO.builder()
                    .roomNo(roomNo)
                    .departments(departments)
                    .subjects(subjects)
                    .rightSeats(rightSeats)
                    .middleSeats(middleSeats)
                    .leftSeats(leftSeats)
                    .build();

            reports.add(report);
        }

        reports.sort(Comparator.comparing(RoomReportDTO::getRoomNo));
        return reports;
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAllArrangementDates() {
        return arrangementRepository.findAllArrangementDates();
    }

    @Transactional
    public void deleteArrangement(LocalDate date) {
        arrangementRepository.deleteByArrangementDate(date);
        resetPreviousArrangement();
        log.info("Deleted arrangement for date: {}", date);
    }
}
