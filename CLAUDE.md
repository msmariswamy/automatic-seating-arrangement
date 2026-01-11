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

The `SeatingArrangementService` implements a **bench-by-bench block distribution strategy**:

1. **Bench-by-bench allocation**: Allocates all three positions (R, M, L) on each bench together before moving to the next bench
2. **Block distribution with subject exhaustion**:
   - Each position (R, M, L) maintains its own current subject
   - Position switches to next subject only when current subject is exhausted (all students allocated)
   - All selected subjects are distributed to ensure complete utilization
3. **Offset strategy for bench constraint**:
   - R positions start with Subject 1
   - M positions start with Subject 2 (offset by 1)
   - L positions start with Subject 3 (offset by 2)
   - Each position independently switches subjects when exhausted

**Key constraint**: No two students with the same subject on the same bench (R≠M≠L for each bench).

**Example with 5 subjects** (Commerce-VI, India in World Politics, History, Economics, Law):
```
Bench 1: R1=Commerce-VI,  M1=India in World Politics, L1=History
Bench 2: R2=Commerce-VI,  M2=India in World Politics, L2=History
Bench 3: R3=Commerce-VI,  M3=India in World Politics, L3=History
...
(When Commerce-VI exhausted in R)
Bench X: RX=Economics,    MX=India in World Politics, LX=History
(When India in World Politics exhausted in M)
Bench Y: RY=Economics,    MY=Law,                     LY=History
```

This bench-by-bench approach ensures:
- All selected subjects are utilized
- Subjects are consumed in continuous blocks per position
- Same-bench constraint is always enforced (R, M, L on same bench allocated together)

#### Algorithm Details

**Subject Selection & Ordering:**
- Subjects are ordered by student count (descending) before allocation
- Subject with most students is allocated first to R positions
- Second-most students allocated to M positions
- Third-most students allocated to L positions

**Allocation Process:**
1. All seats are grouped by bench (using Room ID + Bench Number as key)
2. Benches are sorted in order (Room 1 Bench 1, Room 1 Bench 2, etc.)
3. For each bench:
   - Allocate R position from current R subject (if students available, else switch to next subject)
   - Allocate M position from current M subject (if students available, else switch to next subject)
   - Allocate L position from current L subject (if students available, else switch to next subject)
4. Each position independently tracks and switches subjects when exhausted

**Subject Switching Logic:**
- When a position's current subject is exhausted, it searches for the next available subject
- Searches through all subjects in order (starting from current index + 1)
- Skips subjects that have no remaining unallocated students
- If all subjects exhausted, that position stops allocation

**Edge Cases:**
- **Single Subject**: All positions (R, M, L) use the same subject (constraint cannot be enforced)
- **Two Subjects**: R and L use different subjects, M uses whichever has more students
- **Three+ Subjects**: Full offset strategy applies (R=Sub1, M=Sub2, L=Sub3, etc.)
- **Unequal Student Counts**: Positions with exhausted subjects continue with next available subject
- **Insufficient Seats**: Students without allocated seats are tracked and logged

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

### Verifying Correct Allocation

**Expected Output:**
- Each position (R, M, L) should show continuous blocks of the same subject
- On any single bench, R, M, and L should have different subjects (unless only 1 subject selected)
- All selected subjects should appear in the final allocation
- Application logs show subject switching messages when blocks change

**Sample Log Output:**
```
Students found per subject:
  Commerce-VI -> 45 students
  India in World Politics -> 38 students
  History of Marathas -> 32 students
  Advanced Macroeconomics -> 28 students

Starting allocation - R: Commerce-VI, M: India in World Politics, L: History of Marathas
R-position: Subject Commerce-VI exhausted, switching to next
R-position: Switched to subject Advanced Macroeconomics
M-position: Subject India in World Politics exhausted, switching to next
M-position: Switched to subject Advanced Macroeconomics

Seating allocation complete: 143 students allocated
Subject distribution: {Commerce-VI=45, India in World Politics=38, History of Marathas=32, Advanced Macroeconomics=28}
```

**Red Flags:**
- Same subject appearing on R, M, L of the same bench (indicates constraint violation)
- Selected subjects not appearing in final allocation (indicates algorithm issue)
- Large number of unallocated students when seats are available (indicates logic error)

## Recent Changes

### January 2026 - Algorithm & Build Updates

**Seating Allocation Algorithm Rewrite:**
- **Fixed**: Changed from position-by-position allocation to bench-by-bench allocation
  - **Old behavior**: Allocated ALL R seats first, then ALL M seats, then ALL L seats
  - **Issue**: Same-bench constraint (R≠M≠L) could not be properly enforced
  - **New behavior**: Allocates R, M, L together for each bench before moving to next bench
  - **Result**: Proper enforcement of same-bench constraint with block distribution

- **Fixed**: Subject rotation vs block distribution
  - **Old behavior**: Rotated through subjects (R1→Sub1, R2→Sub2, R3→Sub3, R4→Sub1...)
  - **Issue**: Subjects mixed within same position, no continuous blocks
  - **New behavior**: Block distribution where each position maintains one subject until exhausted
  - **Result**: All selected subjects are utilized, continuous subject blocks per position

**Build Configuration Updates:**
- Upgraded Java from 17 to 21 (build.gradle, gradle.properties)
- Upgraded Gradle from 8.4 to 8.11.1 (gradle-wrapper.properties)
- Upgraded Lombok from default to 1.18.34 for Java 21 compatibility
- Added gradle.properties with JAVA_HOME configuration for Java 21

**File Changes:**
- `SeatingArrangementService.java` - Complete rewrite of allocateSeats() method (lines 79-260)
- `build.gradle` - Java 21 and Lombok 1.18.34
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.11.1
- `gradle.properties` - New file with JAVA_HOME setting
- `CLAUDE.md` - Updated algorithm documentation, tech stack, troubleshooting
