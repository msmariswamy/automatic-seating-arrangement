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

The `SeatingArrangementService` implements a **room-by-room 2-subject-per-bench strategy**:

**Key Rules:**
1. **R and L must have DIFFERENT subjects** (R ≠ L on each bench)
2. **M must match EITHER R or L** (M = R's subject OR M = L's subject)
3. **Sequential processing**: Process rooms in order (Room 1, Room 2, ...), then benches in order (Bench 1, Bench 2, ...)
4. **Complete one room** before moving to the next
5. **Subject sequence continues** from one room to the next (maintains subject blocks across rooms)

**Pattern:** Each bench has exactly **2 subjects total**, with M duplicating either R or L's subject.

**Example with 2 subjects** (Commerce-VI has 50 students, History has 40 students):
```
Room 1:
  Bench 1: R1=Commerce-VI,  M1=Commerce-VI,  L1=History    (M matches R)
  Bench 2: R2=Commerce-VI,  M2=Commerce-VI,  L2=History    (M matches R)
  Bench 3: R3=Commerce-VI,  M3=History,      L3=History    (M matches L)
  ...
  (When Commerce-VI exhausted in R)
  Bench 10: R10=(no more), M10=History,     L10=History   (M matches L)

Room 2:
  Bench 1: R1=(next subject), M1=History,   L1=History
  ...
```

**M Position Logic:**
- M checks which subject (R or L) has more unallocated students
- M allocates from that subject to maximize utilization
- If one subject is exhausted, M automatically uses the other

This strategy ensures:
- **2 subjects per bench**: Exactly two different subjects on each bench (R≠L, M matches one)
- **Sequential seat allocation**: Starts from R1/M1/L1 in each room
- **Continuous subject blocks**: Each position maintains subject until exhausted
- **Maximum utilization**: M fills gaps by matching whichever subject has more students

#### Algorithm Details

**Subject Selection & Ordering:**
- Subjects are ordered by student count (descending) before allocation
- Subject with most students is allocated to R positions
- Subject with second-most students is allocated to L positions
- M matches whichever (R or L) has more students available at each bench

**Allocation Process:**
1. Rooms are sorted by room number (Room 1, Room 2, Room 3, ...)
2. For each room in order:
   - Get all seats for this room
   - Group seats by bench number
   - Sort benches in order (Bench 1, Bench 2, Bench 3, ...)
3. For each bench in order:
   - **Allocate R position** from current R subject (if students available, else switch to next subject ≠ L)
   - **Allocate L position** from current L subject (if students available, else switch to next subject ≠ R)
   - **Allocate M position** from whichever subject (R or L) has more unallocated students
4. Subject sequence continues from one room to the next (R and L maintain their subject blocks across room boundaries)

**Subject Switching Logic:**
- **R position switching:**
  - When R's current subject is exhausted, search for next available subject
  - Skip L's current subject to maintain R≠L constraint
  - Continue to next subject in priority order
- **L position switching:**
  - When L's current subject is exhausted, search for next available subject
  - Skip R's current subject to maintain R≠L constraint
  - Continue to next subject in priority order
- **M position logic:**
  - M always matches EITHER R or L (never a third subject)
  - Count unallocated students in R's subject vs L's subject
  - M uses whichever has more students available
  - If preferred subject exhausted, M automatically uses the alternate (R or L)

**Edge Cases:**
- **Single Subject**: Error - requires at least 2 subjects for R≠L constraint
- **Two Subjects**: Ideal case - R=Sub1, L=Sub2, M matches whichever has more students
- **Three+ Subjects**: R uses Sub1 until exhausted then Sub3, L uses Sub2 until exhausted then Sub4, M matches R or L
- **Unequal Student Counts**: M dynamically balances by matching the subject with more students on each bench
- **Insufficient Seats**: Unallocated students are tracked and logged

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
- `reports.html` - Consolidated and individual room reports with configurable settings
  - Settings section with toggleable options (Show Subject column)
  - Designed for future setting additions

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
- Each bench should have exactly **2 subjects** (R≠L, M matches either R or L)
- Seats should start from R1/M1/L1 in each room (sequential allocation)
- Each position (R, L) should show continuous blocks of the same subject
- M should alternate between R's subject and L's subject based on availability
- Rooms should be processed in order (Room 1, then Room 2, then Room 3, ...)

**Sample Log Output:**
```
Students found per subject:
  Commerce-VI -> 50 students
  History -> 40 students

Starting allocation - R: Commerce-VI, L: History, M: will match R or L
Processing room: Room 1
Processing room: Room 2
R-position: Subject Commerce-VI exhausted, switching to next
R-position: Switched to subject History
L-position: Subject History exhausted, switching to next

Seating allocation complete: 90 students allocated
Subject distribution: {Commerce-VI=50, History=40}
```

**Red Flags:**
- **R and L having the same subject on any bench** (violates R≠L constraint)
- **M having a subject different from both R and L** (violates M=R or M=L rule)
- **Seats starting at R9 instead of R1** (indicates sequencing issue)
- **Large number of unallocated students when seats are available** (indicates logic error)

**Correct Pattern Example:**
```
Room 24:
  Bench 1: R1=Commerce-VI,  M1=Commerce-VI,  L1=History     ✓ (M matches R, R≠L)
  Bench 2: R2=Commerce-VI,  M2=History,      L2=History     ✓ (M matches L, R≠L)
  Bench 3: R3=Commerce-VI,  M3=Commerce-VI,  L3=History     ✓ (M matches R, R≠L)
```

**Incorrect Pattern Example:**
```
Room 24:
  Bench 1: R9=Commerce-VI,  M1=Commerce-VI,  L1=(No seats)  ✗ (Should start at R1, L missing)
```

## Recent Changes

### January 2026 - Complete Algorithm Rewrite

**Major Algorithm Changes - 2-Subject-Per-Bench Strategy:**

- **COMPLETE REWRITE**: Changed to room-by-room 2-subject-per-bench allocation
  - **Old behavior**: R, M, L all had different subjects (3 subjects per bench with offset strategy)
  - **New requirement**: R≠L, M matches either R or L (2 subjects per bench)
  - **New behavior**:
    - R and L maintain different subjects (continuous blocks)
    - M dynamically matches whichever (R or L) has more students available
    - Rooms processed sequentially (Room 1, Room 2, ...)
    - Benches processed in order within each room (Bench 1, Bench 2, ...)
    - Subject sequence continues across room boundaries
  - **Result**: Each bench has exactly 2 subjects, M fills gaps intelligently

- **Fixed**: Sequential seat allocation
  - **Old issue**: Seats starting at R9 instead of R1 (sequencing problem)
  - **Solution**: Process rooms in order, then benches in order within each room
  - **Result**: Seats now start from R1/M1/L1 in each room

- **Fixed**: Subject constraint logic
  - **Old constraint**: R≠M≠L (all different on same bench)
  - **New constraint**: R≠L, M=R OR M=L (2 subjects per bench)
  - **Implementation**:
    - R and L check each other when switching subjects
    - M counts available students in R vs L subjects and matches the higher
    - If preferred subject exhausted, M automatically uses alternate
  - **Result**: Proper 2-subject allocation with intelligent M balancing

- **Eliminated**: 3-subject offset strategy
  - **Old behavior**: R=Sub1, M=Sub2, L=Sub3 (offset by 1)
  - **Why removed**: New requirement is 2 subjects per bench, not 3
  - **New behavior**: R=Sub1, L=Sub2, M matches whichever has more students

**Build Configuration Updates:**
- Upgraded Java from 17 to 21 (build.gradle, gradle.properties)
- Upgraded Gradle from 8.4 to 8.11.1 (gradle-wrapper.properties)
- Upgraded Lombok from default to 1.18.34 for Java 21 compatibility
- Added gradle.properties with JAVA_HOME configuration for Java 21

**UI Enhancements:**
- **Added**: Settings section on reports page (/reports.html)
  - Checkbox to show/hide Subject column in room reports
  - Positioned at top of page for easy access
  - Designed with extensibility in mind for future settings (e.g., show/hide departments, font size, etc.)
  - Uses CSS classes and jQuery for dynamic column toggling
  - Subject column shown by default (checkbox checked)

**File Changes:**
- `SeatingArrangementService.java` - Complete rewrite of allocateSeats() method (lines 79-276)
- `reports.html` - Added Settings section with Show Subject checkbox
- `build.gradle` - Java 21 and Lombok 1.18.34
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.11.1
- `gradle.properties` - New file with JAVA_HOME setting
- `CLAUDE.md` - Updated algorithm documentation, tech stack, troubleshooting
