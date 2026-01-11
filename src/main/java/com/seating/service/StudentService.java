package com.seating.service;

import com.seating.dto.StudentDTO;
import com.seating.entity.Student;
import com.seating.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * Service for managing students
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final ExcelService excelService;

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<String> getAllDepartments() {
        return studentRepository.findAllDepartments();
    }

    @Transactional(readOnly = true)
    public List<String> getAllClasses() {
        return studentRepository.findAllClasses();
    }

    @Transactional(readOnly = true)
    public List<String> getAllSubjects() {
        return studentRepository.findAllSubjects();
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getDepartmentSubjectMapping() {
        List<String> departments = studentRepository.findAllDepartments();
        Map<String, List<String>> mapping = new HashMap<>();

        for (String department : departments) {
            List<String> subjects = studentRepository.findSubjectsByDepartment(department);
            mapping.put(department, subjects);
        }

        return mapping;
    }

    @Transactional(readOnly = true)
    public long getStudentCount() {
        return studentRepository.count();
    }

    @Transactional
    public void uploadStudents(MultipartFile file) throws Exception {
        try {
            List<StudentDTO> studentDTOs = excelService.parseStudentExcel(file);

            if (studentDTOs.isEmpty()) {
                throw new IllegalArgumentException("No valid student data found in Excel file");
            }

            int savedCount = 0;
            int skippedCount = 0;

            for (StudentDTO dto : studentDTOs) {
                if (studentRepository.existsByRollNo(dto.getRollNo())) {
                    log.warn("Student with Roll No {} already exists, skipping", dto.getRollNo());
                    skippedCount++;
                    continue;
                }

                Student student = Student.builder()
                        .rollNo(dto.getRollNo())
                        .name(dto.getName())
                        .department(dto.getDepartment())
                        .className(dto.getClassName())
                        .subjects(dto.getSubjects())
                        .isAllocated(false)
                        .build();

                studentRepository.save(student);
                savedCount++;
            }

            log.info("Student upload completed. Saved: {}, Skipped: {}", savedCount, skippedCount);

        } catch (Exception e) {
            log.error("Error uploading students: {}", e.getMessage(), e);
            throw new Exception("Failed to upload students: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Student> getFilteredStudents(Set<String> departments, Set<String> classes, Set<String> subjects) {
        if (departments == null || departments.isEmpty() ||
            classes == null || classes.isEmpty() ||
            subjects == null || subjects.isEmpty()) {
            return List.of();
        }

        return studentRepository.findByDepartmentsAndClassesAndSubjects(departments, classes, subjects);
    }

    @Transactional
    public void resetAllocations() {
        studentRepository.resetAllAllocations();
        log.info("All student allocations have been reset");
    }

    @Transactional
    public void deleteAllStudents() {
        studentRepository.deleteAllStudents();
        log.info("All students have been deleted");
    }
}
