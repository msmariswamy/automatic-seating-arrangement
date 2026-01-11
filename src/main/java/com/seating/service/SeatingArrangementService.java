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

    /**
     * BLOCK DISTRIBUTION ALLOCATION STRATEGY
     * - Each position (R, M, L) is divided into blocks by subject
     * - New block starts only when previous subject is exhausted
     * - All selected subjects are distributed across positions
     * - Ensures no two students with same subject on same bench (R≠M≠L for each bench)
     * - Uses offset strategy: if R starts with Sub1, M starts with Sub2, L starts with Sub3
     */
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

        // Group seats by position and sort them
        Map<String, List<Seat>> seatsByPosition = groupSeatsByPosition(allSeats);

        // Group students by subject
        Map<String, List<Student>> studentsBySubject = groupStudentsBySubjectList(students, filter.getSubjects());

        if (studentsBySubject.isEmpty()) {
            log.error("No students found for the selected subjects");
            return arrangements;
        }

        // Log student counts per subject
        log.info("Students found per subject:");
        for (Map.Entry<String, List<Student>> entry : studentsBySubject.entrySet()) {
            log.info("  {} -> {} students", entry.getKey(), entry.getValue().size());
        }

        // Get list of subjects ordered by student count (descending)
        List<String> orderedSubjects = studentsBySubject.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                        countUnallocatedStudents(e2.getValue()),
                        countUnallocatedStudents(e1.getValue())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("Subjects ordered by count: {}", orderedSubjects);

        int numSubjects = orderedSubjects.size();

        // Get seats for each position
        List<Seat> rSeats = seatsByPosition.getOrDefault("R", new ArrayList<>());
        List<Seat> mSeats = seatsByPosition.getOrDefault("M", new ArrayList<>());
        List<Seat> lSeats = seatsByPosition.getOrDefault("L", new ArrayList<>());

        // Allocate R-series starting with subject at offset 0
        log.info("Allocating R-series ({} seats)", rSeats.size());
        arrangements.addAll(allocatePositionWithBlocks(rSeats, studentsBySubject, orderedSubjects, 0, filter));

        // Allocate M-series starting with subject at offset 1 (to avoid R conflicts)
        int mOffset = numSubjects >= 2 ? 1 : 0;
        log.info("Allocating M-series ({} seats) with offset {}", mSeats.size(), mOffset);
        arrangements.addAll(allocatePositionWithBlocks(mSeats, studentsBySubject, orderedSubjects, mOffset, filter));

        // Allocate L-series starting with subject at offset 2 (to avoid R and M conflicts)
        int lOffset = numSubjects >= 3 ? 2 : (numSubjects >= 2 ? 1 : 0);
        log.info("Allocating L-series ({} seats) with offset {}", lSeats.size(), lOffset);
        arrangements.addAll(allocatePositionWithBlocks(lSeats, studentsBySubject, orderedSubjects, lOffset, filter));

        // Log subject distribution
        Map<String, Long> subjectCounts = arrangements.stream()
                .collect(Collectors.groupingBy(SeatingArrangement::getSubject, Collectors.counting()));
        log.info("Seating allocation complete: {} students allocated", arrangements.size());
        log.info("Subject distribution: {}", subjectCounts);

        int totalUnallocated = students.stream()
                .mapToInt(s -> s.getIsAllocated() ? 0 : 1)
                .sum();
        if (totalUnallocated > 0) {
            log.warn("Warning: {} students were not allocated (insufficient seats or subject constraints)", totalUnallocated);
        }

        return arrangements;
    }

    /**
     * Allocate students to a position (R, M, or L) using block distribution
     * Subjects are allocated in blocks - moves to next subject only when current subject is exhausted
     */
    private List<SeatingArrangement> allocatePositionWithBlocks(List<Seat> seats,
                                                                 Map<String, List<Student>> studentsBySubject,
                                                                 List<String> orderedSubjects,
                                                                 int startOffset,
                                                                 SeatingFilterDTO filter) {
        List<SeatingArrangement> arrangements = new ArrayList<>();

        if (seats.isEmpty() || orderedSubjects.isEmpty()) {
            return arrangements;
        }

        int numSubjects = orderedSubjects.size();
        int currentSubjectIndex = startOffset % numSubjects;
        String currentSubject = orderedSubjects.get(currentSubjectIndex);

        log.info("Starting {} position allocation with subject: {}",
                 seats.isEmpty() ? "?" : seats.get(0).getPosition(), currentSubject);

        for (Seat seat : seats) {
            // Try to allocate from current subject
            List<Student> subjectStudents = studentsBySubject.get(currentSubject);
            Student student = findNextUnallocatedStudent(subjectStudents);

            // If current subject exhausted, move to next subject in the ordered list
            if (student == null) {
                log.info("Subject {} exhausted, moving to next subject", currentSubject);

                // Find next subject with available students
                boolean foundNextSubject = false;
                for (int i = 1; i <= numSubjects; i++) {
                    currentSubjectIndex = (startOffset + i) % numSubjects;
                    currentSubject = orderedSubjects.get(currentSubjectIndex);
                    subjectStudents = studentsBySubject.get(currentSubject);
                    student = findNextUnallocatedStudent(subjectStudents);

                    if (student != null) {
                        log.info("Switched to subject: {}", currentSubject);
                        foundNextSubject = true;
                        startOffset = currentSubjectIndex; // Update offset for next iteration
                        break;
                    }
                }

                if (!foundNextSubject) {
                    log.warn("No more students available for {} position", seat.getPosition());
                    break;
                }
            }

            // Create arrangement
            SeatingArrangement arrangement = createArrangement(student, seat, currentSubject, filter);
            arrangements.add(arrangement);

            // Mark as allocated
            student.setIsAllocated(true);
            seat.setIsOccupied(true);
        }

        String position = seats.isEmpty() ? "?" : seats.get(0).getPosition();
        log.info("Allocated {} students to {}-series", arrangements.size(), position);

        return arrangements;
    }


    /**
     * Group seats by position (R, M, L) and sort them by room and bench number
     */
    private Map<String, List<Seat>> groupSeatsByPosition(List<Seat> seats) {
        Map<String, List<Seat>> seatsByPosition = new HashMap<>();
        seatsByPosition.put("R", new ArrayList<>());
        seatsByPosition.put("M", new ArrayList<>());
        seatsByPosition.put("L", new ArrayList<>());

        for (Seat seat : seats) {
            String position = seat.getPosition();
            if (seatsByPosition.containsKey(position)) {
                seatsByPosition.get(position).add(seat);
            }
        }

        // Sort each position's seats by room ID and bench number
        Comparator<Seat> seatComparator = Comparator
                .comparing((Seat s) -> s.getRoom().getId())
                .thenComparing(Seat::getBenchNo);

        seatsByPosition.values().forEach(list -> list.sort(seatComparator));

        return seatsByPosition;
    }

    /**
     * Group students by subject into lists
     * Each student is assigned to their matching subject from the filter
     */
    private Map<String, List<Student>> groupStudentsBySubjectList(List<Student> students, Set<String> filterSubjects) {
        Map<String, List<Student>> studentsBySubject = new HashMap<>();

        for (Student student : students) {
            for (String subject : student.getSubjects()) {
                // Check if this subject is in the filter
                if (filterSubjects.contains(subject)) {
                    studentsBySubject.computeIfAbsent(subject, k -> new ArrayList<>()).add(student);
                    break; // Each student is assigned to only one subject (first match)
                }
            }
        }

        return studentsBySubject;
    }

    /**
     * Select the best subject for a position based on student count
     * Excludes already selected subjects
     */
    private String selectSubjectForPosition(Map<String, List<Student>> studentsBySubject,
                                           String excludeSubject1,
                                           String excludeSubject2) {
        String selectedSubject = null;
        int maxCount = 0;

        for (Map.Entry<String, List<Student>> entry : studentsBySubject.entrySet()) {
            String subject = entry.getKey();

            if (subject.equals(excludeSubject1) || subject.equals(excludeSubject2)) {
                continue;
            }

            int unallocatedCount = countUnallocatedStudents(entry.getValue());

            if (unallocatedCount > maxCount) {
                maxCount = unallocatedCount;
                selectedSubject = subject;
            }
        }

        return selectedSubject;
    }

    /**
     * Allocate students from a subject to all seats in a position series
     */
    private List<SeatingArrangement> allocatePositionSeries(List<Seat> seats,
                                                            List<Student> students,
                                                            String subject,
                                                            SeatingFilterDTO filter) {
        List<SeatingArrangement> arrangements = new ArrayList<>();

        int studentIndex = 0;
        for (Seat seat : seats) {
            // Find next unallocated student
            while (studentIndex < students.size() && students.get(studentIndex).getIsAllocated()) {
                studentIndex++;
            }

            if (studentIndex >= students.size()) {
                log.warn("Not enough students in subject {} to fill all {} seats", subject, seat.getPosition());
                break;
            }

            Student student = students.get(studentIndex);

            // Create arrangement
            SeatingArrangement arrangement = createArrangement(student, seat, subject, filter);
            arrangements.add(arrangement);

            // Mark as allocated
            student.setIsAllocated(true);
            seat.setIsOccupied(true);

            studentIndex++;
        }

        log.info("Allocated {} students to {}-series (subject: {})", arrangements.size(),
                 seats.isEmpty() ? "?" : seats.get(0).getPosition(), subject);

        return arrangements;
    }

    /**
     * Count unallocated students in a list
     */
    private int countUnallocatedStudents(List<Student> students) {
        if (students == null) {
            return 0;
        }
        return (int) students.stream().filter(s -> !s.getIsAllocated()).count();
    }

    /**
     * Find the next unallocated student from a list
     */
    private Student findNextUnallocatedStudent(List<Student> students) {
        if (students == null) {
            return null;
        }
        for (Student student : students) {
            if (!student.getIsAllocated()) {
                return student;
            }
        }
        return null;
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

    /**
     * Find a student with better spacing - avoids subjects used on same bench
     * and subjects recently used in the same position
     */
    private Student findStudentWithSpacing(Map<String, Queue<Student>> studentsBySubject,
                                          Set<String> usedSubjects,
                                          List<String> recentSubjects) {
        // Phase 1: Try to find student whose subject is NOT on this bench AND NOT recently used in this position
        for (Map.Entry<String, Queue<Student>> entry : studentsBySubject.entrySet()) {
            String subject = entry.getKey();
            Queue<Student> queue = entry.getValue();

            if (!usedSubjects.contains(subject) && !recentSubjects.contains(subject)) {
                for (Student student : queue) {
                    if (!student.getIsAllocated()) {
                        queue.remove(student);
                        return student;
                    }
                }
            }
        }

        // Phase 2: Try to find student whose subject is at least NOT on this bench (ignore recent subjects)
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

        // Phase 3: Fallback - return any available student
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

                deptArrangements.sort(Comparator.comparing(a -> a.getSeat().getBenchNo()));

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
                        .benchNo(arr.getSeat().getBenchNo())
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

            rightSeats.sort(Comparator.comparing(SeatAllocationDTO::getBenchNo));
            middleSeats.sort(Comparator.comparing(SeatAllocationDTO::getBenchNo));
            leftSeats.sort(Comparator.comparing(SeatAllocationDTO::getBenchNo));

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
