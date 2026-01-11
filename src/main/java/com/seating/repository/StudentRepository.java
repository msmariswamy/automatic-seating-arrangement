package com.seating.repository;

import com.seating.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Student entity
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRollNo(String rollNo);

    boolean existsByRollNo(String rollNo);

    List<Student> findByDepartment(String department);

    List<Student> findByClassName(String className);

    @Query("SELECT DISTINCT s.department FROM Student s ORDER BY s.department")
    List<String> findAllDepartments();

    @Query("SELECT DISTINCT s.className FROM Student s ORDER BY s.className")
    List<String> findAllClasses();

    @Query("SELECT DISTINCT sub FROM Student s JOIN s.subjects sub ORDER BY sub")
    List<String> findAllSubjects();

    @Query("SELECT DISTINCT sub FROM Student s JOIN s.subjects sub WHERE s.department = :department ORDER BY sub")
    List<String> findSubjectsByDepartment(@Param("department") String department);

    @Query("SELECT s FROM Student s WHERE s.department IN :departments " +
           "AND s.className IN :classes " +
           "AND EXISTS (SELECT 1 FROM s.subjects sub WHERE sub IN :subjects)")
    List<Student> findByDepartmentsAndClassesAndSubjects(
        @Param("departments") Set<String> departments,
        @Param("classes") Set<String> classes,
        @Param("subjects") Set<String> subjects
    );

    @Query("SELECT s FROM Student s WHERE s.isAllocated = false")
    List<Student> findUnallocatedStudents();

    @Modifying
    @Query("UPDATE Student s SET s.isAllocated = false")
    void resetAllAllocations();

    @Modifying
    @Query("DELETE FROM Student")
    void deleteAllStudents();
}
