# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Automatic Seating Arrangement System - A Spring Boot web application for managing exam seating arrangements with constraint-based allocation algorithms. The system ensures no two students with the same subject sit on the same bench.

### Tech Stack
- **Backend:** Java 21, Spring Boot 3.2.2
- **Database:** PostgreSQL 15+
- **Excel Processing:** Apache POI 5.2.5
- **PDF Generation:** iText 5.5.13.3
- **Frontend:** Thymeleaf, Bootstrap 5, jQuery 3.7
- **Build Tool:** Gradle 8.11.1
- **Lombok:** 1.18.34 (required for Java 21+ compatibility)

## Build & Run Commands

**Prerequisites**: Java 21 is required. Set JAVA_HOME to Java 21:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home
```

Or configure in `gradle.properties`:
```properties
org.gradle.java.home=/path/to/java-21
```

### Build
```bash
./gradlew clean build
```

### Run Application
```bash
./gradlew bootRun
```

Or run the JAR directly:
```bash
java -jar build/libs/automatic-seating-arrangement-1.0.0.jar
```

### Testing
```bash
./gradlew test
```

### Access Application
- URL: http://localhost:8080
- Default credentials: admin/admin123

## Database Setup

The application uses PostgreSQL. Database schema and sample data are in `database/init.sql`.

### Initialize Database
```bash
psql -U postgres -f database/init.sql
```

### Configuration
Database settings are in `src/main/resources/application.properties`:
- Default database: `postgres` (not `seating_db` - ensure this matches your setup)
- Default credentials: postgres/postgres

## Architecture

### Core Seating Algorithm

The `SeatingArrangementService` implements a **block distribution allocation strategy**:

1. **Position-based allocation**: Seats are grouped by position (R=Right, M=Middle, L=Left)
2. **Block distribution with subject exhaustion**:
   - Each position (R, M, L) is allocated in continuous blocks by subject
   - New subject block starts only when the previous subject is exhausted (all students allocated)
   - All selected subjects are distributed across positions to ensure complete utilization
3. **Offset strategy for bench constraint**:
   - R positions start with Subject 1
   - M positions start with Subject 2 (offset by 1)
   - L positions start with Subject 3 (offset by 2)
   - This ensures different subjects on each bench position

**Key constraint**: No two students with the same subject on the same bench (R≠M≠L for each bench).

**Example**: If 5 subjects are selected:
- R-series: Sub1 (until exhausted) → Sub2 (until exhausted) → Sub3...
- M-series: Sub2 (until exhausted) → Sub3 (until exhausted) → Sub4...
- L-series: Sub3 (until exhausted) → Sub4 (until exhausted) → Sub5...

This approach ensures all selected subjects are used in the allocation, unlike strategies that only use 2-3 subjects.

### Entity Relationships

```
Student (1) ←→ (M) StudentSubjects
Student (1) → (M) SeatingArrangement
Room (1) → (M) Seats
Room (1) → (M) SeatingArrangement
Seat (1) → (M) SeatingArrangement
```

**Critical fields**:
- `Student.isAllocated` (Boolean) - Tracks allocation state, reset on new arrangement
- `Seat.isOccupied` (Boolean) - Tracks seat occupancy, reset on new arrangement
- `Seat.position` - 'R', 'M', or 'L' (Right, Middle, Left on bench)
- `Seat.benchNo` - Groups seats into benches (3 seats per bench: R, M, L)

### Package Structure

- `config/` - Spring Security configuration with BCrypt password encoding
- `controller/` - REST endpoints for students, rooms, seating arrangements, and reports
- `entity/` - JPA entities with Lombok (all extend `BaseEntity` for ID/timestamps)
- `repository/` - Spring Data JPA repositories with custom query methods
- `service/` - Business logic layer:
  - `SeatingArrangementService` - Core allocation algorithm
  - `ExcelService` - Apache POI for Excel template generation and parsing
  - `PdfService` - iText for PDF report generation
  - `StudentService` - Student management with multi-subject support
  - `RoomService` - Room and seat management
- `dto/` - Data Transfer Objects for API responses and filtering

### Template Layer

Thymeleaf templates in `src/main/resources/templates/`:
- `login.html` - Authentication page
- `dashboard.html` - Main interface for upload/generation
- `reports.html` - Consolidated and individual room reports

## API Endpoints Reference

### Student Management
- `POST /api/students/upload` - Upload student Excel file
- `GET /api/students/template` - Download Excel template
- `GET /api/students/departments` - Get all departments
- `GET /api/students/classes` - Get all classes
- `GET /api/students/subjects` - Get all subjects
- `DELETE /api/students` - Delete all students

### Room Management
- `POST /api/rooms/upload` - Upload room Excel file
- `GET /api/rooms/template` - Download Excel template
- `DELETE /api/rooms` - Delete all rooms and seats

### Seating Arrangement
- `POST /api/seating/generate` - Generate seating arrangement (requires SeatingFilterDTO)
- `GET /api/seating/reports/consolidated` - Get department-wise summary by room
- `GET /api/seating/reports/rooms` - Get detailed room reports
- `GET /api/seating/reports/room/pdf?roomNo={roomNo}&date={date}` - Download room PDF
- `GET /api/seating/dates` - Get all arrangement dates
- `DELETE /api/seating?date={date}` - Delete arrangement by date

## Development Notes

### Excel Upload Format

**Students Template**: Roll No, Name, Department, Class, Subject1, Subject2, Subject3, Subject4, Subject5
- Roll No must be unique
- Each student can have 1-5 subjects
- First matching subject from filter criteria is used for allocation
- Max file size: 10MB

**Rooms Template**: Room No, Total Benches, Capacity, R Count, M Count, L Count
- Room No must be unique
- Capacity must equal R Count + M Count + L Count
- Seats are auto-generated as R1, M1, L1, R2, M2, L2, etc.
- R/M/L represent Right/Middle/Left positions on each bench

### Report Generation

Two report types:
1. **Consolidated Report**: Department-wise summary per room with seat ranges
2. **Individual Room Reports**: Three-column layout (Right/Middle/Left) showing detailed allocations

PDF generation uses iText 5.5.13 (older version, not iText 7).

### Security

Spring Security with:
- BCrypt password hashing
- Session-based authentication
- In-memory user details (default user in application.properties)
- Database user table exists but not currently used for authentication

### Known Patterns

- All services use `@Slf4j` for logging
- All entities extend `BaseEntity` (provides id, createdAt, updatedAt)
- DTOs use Lombok `@Builder` pattern
- Repositories use Spring Data JPA with custom `@Query` annotations for complex filtering
- Transactional boundaries at service layer (`@Transactional`)

### Reset Behavior

Generating a new arrangement resets:
- All `Student.isAllocated` flags to false
- All `Seat.isOccupied` flags to false

This means only one arrangement can be "active" at a time, but historical arrangements are preserved in the `seating_arrangements` table by date.

## Common Workflow

1. **Upload Students**: Download template → Fill data → Upload Excel
2. **Upload Rooms**: Download template → Fill room configuration → Upload Excel
3. **Generate Arrangement**: Select departments, classes, subjects → Generate
4. **View Reports**: Select date → View consolidated or individual room reports → Download PDFs as needed
5. **Delete Data**: Delete students/rooms/arrangements as needed to reset the system

## Troubleshooting

### Database Issues
- Verify PostgreSQL is running: `psql -U postgres -c "SELECT version();"`
- Check database connection settings in `application.properties`
- Database name is `postgres` by default (not `seating_db`)

### Excel Upload Failures
- Ensure file format is .xlsx or .xls
- Verify template headers match exactly (case-sensitive)
- Check max file size (10MB limit)
- Ensure no special characters in Room No or Roll No fields

### Seating Generation Returns No Results
- Verify students and rooms exist in database
- Ensure filter criteria (departments, classes, subjects) match actual student data
- Check that rooms have available seats (not all occupied)
- Review application logs for detailed error messages (logging level is DEBUG for com.seating package)
