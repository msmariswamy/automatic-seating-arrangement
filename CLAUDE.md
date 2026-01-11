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

The `SeatingArrangementService` implements a **room-by-room position-by-position 2-subject-per-bench strategy**:

**Quick Summary:**
- **Allocation Order**: Room 11 (R→M→L) → Room 101 (R→M→L) → Room 105 (R→M→L) → Room 106 (R→M→L)
- **Each Room**: Phase 1: All R seats → Phase 2: All M seats → Phase 3: All L seats
- **Constraint**: R≠L (different subjects), M=R OR M=L (matches one of them)
- **Continuity**: R and L subjects continue seamlessly from room to room

**Key Rules:**
1. **R and L must have DIFFERENT subjects** (R ≠ L on each bench)
2. **M must match EITHER R or L** (M = R's subject OR M = L's subject)
3. **Sequential processing**: Rooms in ID order, seats in bench order (R1, R2, R3... then M1, M2, M3... then L1, L2, L3...)
4. **Complete one room** before moving to the next (all R, M, L in Room 11 done before Room 101 starts)
5. **Subject sequence continues** from one room to the next (R and L subjects maintain across room boundaries)

**Pattern:** Each bench has exactly **2 subjects total**, with M duplicating either R or L's subject.

**Example with 2 subjects** (Commerce-VI has 50 students, India in World Politics has 40 students):

**Room 11 (ID=1) - Allocation Order:**
```
Phase 1: Allocate ALL R seats
  R1  = AF23001  (Commerce-VI)
  R2  = BA23132  (Commerce-VI)
  R3  = BC20013  (Commerce-VI)
  R4  = BC20017  (Commerce-VI)
  ...
  R11 = BC21408  (Commerce-VI)

Phase 2: Allocate ALL M seats
  M1  = (no seat in Room 11)
  M2  = (no seat in Room 11)
  ... (Room 11 has no M seats)

Phase 3: Allocate ALL L seats
  L1  = BA21131  (India in World Politics)
  L2  = BA22110  (India in World Politics)
  L3  = BA23007  (India in World Politics)
  ...
  L11 = BA23038  (India in World Politics)
```

**Room 101 (ID=2) - Subjects continue:**
```
Phase 1: Allocate ALL R seats (continues Commerce-VI)
  R1  = BC21409  (Commerce-VI - next student after Room 11)
  R2  = BC21410  (Commerce-VI)
  ...

Phase 2: Allocate ALL M seats (matches R or L)
  M1  = BC21415  (Commerce-VI - matches R, more students available)
  M2  = BC21416  (Commerce-VI)
  ...

Phase 3: Allocate ALL L seats (continues India in World Politics)
  L1  = BA23039  (India in World Politics - next student after Room 11)
  L2  = BA23040  (India in World Politics)
  ...
```

**Key Points:**
- Room 11 completed fully (R→M→L) before Room 101 starts
- Commerce-VI subject continues from Room 11 to Room 101 seamlessly
- India in World Politics subject continues from Room 11 to Room 101 seamlessly
- M seats match whichever (R or L) has more students available on each bench

**Workflow Visualization:**
```
Start
  ↓
Initialize: R=Commerce-VI, L=India in World Politics
  ↓
┌─────────────────────────────────────────────┐
│ ROOM 11 (ID=1)                              │
│  Phase 1: R1, R2, R3... (Commerce-VI)       │
│  Phase 2: (no M seats in Room 11)           │
│  Phase 3: L1, L2, L3... (India in World P.) │
└─────────────────────────────────────────────┘
  ↓ (Subjects continue)
┌─────────────────────────────────────────────┐
│ ROOM 101 (ID=2)                             │
│  Phase 1: R1, R2, R3... (Commerce-VI cont.) │
│  Phase 2: M1, M2, M3... (matches R or L)    │
│  Phase 3: L1, L2, L3... (India in World P.) │
└─────────────────────────────────────────────┘
  ↓ (Subjects continue)
┌─────────────────────────────────────────────┐
│ ROOM 105 (ID=3)                             │
│  Phase 1: R1, R2, R3... (Commerce-VI cont.) │
│  Phase 2: M1, M2, M3... (matches R or L)    │
│  Phase 3: L1, L2, L3... (India in World P.) │
└─────────────────────────────────────────────┘
  ↓ (Subjects continue)
┌─────────────────────────────────────────────┐
│ ROOM 106 (ID=4)                             │
│  Phase 1: R1, R2, R3... (Commerce-VI cont.) │
│  Phase 2: M1, M2, M3... (matches R or L)    │
│  Phase 3: L1, L2, L3... (India in World P.) │
└─────────────────────────────────────────────┘
  ↓
Complete
```

This strategy ensures:
- **2 subjects per bench**: Exactly two different subjects on each bench (R≠L, M matches one)
- **Room completion**: Each room fully allocated (R→M→L) before moving to next room
- **Continuous subject blocks**: R and L subjects maintained across room boundaries
- **Maximum utilization**: M fills gaps by matching whichever subject has more students

#### Algorithm Details

**Subject Selection & Ordering:**
- Subjects are ordered by student count (descending) before allocation
- Subject with most students is allocated to R positions
- Subject with second-most students is allocated to L positions
- M matches whichever (R or L) has more students available at each bench

**Allocation Process (Room-by-Room, Position-by-Position):**
1. Rooms are sorted by room ID (numeric order: 11 → 101 → 105 → 106)
2. Subject trackers (R subject, L subject) persist across all rooms
3. **For EACH room in order:**
   - **Phase 1 (Per Room): Allocate ALL R seats in this room**
     - Process R1, R2, R3... in sequence within the room
     - Use current R subject (continues from previous room)
     - If R subject exhausted, switch to next available subject (skip L's subject to maintain R≠L)
   - **Phase 2 (Per Room): Allocate ALL M seats in this room**
     - Process M1, M2, M3... in sequence within the room
     - For each M seat, find corresponding R and L on same bench
     - M matches whichever (R or L) has more unallocated students
     - If only R exists (no L), M matches R; if only L exists (no R), M matches L
   - **Phase 3 (Per Room): Allocate ALL L seats in this room**
     - Process L1, L2, L3... in sequence within the room
     - Use current L subject (continues from previous room)
     - If L subject exhausted, switch to next available subject (skip R's subject to maintain R≠L)
4. Move to next room and repeat (subject sequence continues)

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
- **Room-by-room processing**: Each room fully allocated (R→M→L) before moving to next room
- **Each bench has exactly 2 subjects**: R≠L, M matches either R or L
- **Sequential seats in each room**: R1, R2, R3... then M1, M2, M3... then L1, L2, L3...
- **Continuous subject blocks**: R and L maintain same subject across room boundaries
- **Rooms processed in numeric ID order**: Room 11 → Room 101 → Room 105 → Room 106

**Sample Log Output:**
```
Students found per subject:
  Commerce-VI -> 50 students
  India in World Politics -> 40 students

Subjects ordered by count: [Commerce-VI, India in World Politics]
Starting allocation - R: Commerce-VI, L: India in World Politics, M: will match R or L

=== Processing Room: 11 ===
Room 11 seats - R: 11, M: 0, L: 11
  Phase 1: Allocating R seats in room 11
  Phase 2: Allocating M seats in room 11
  Phase 3: Allocating L seats in room 11
=== Completed Room: 11 ===

=== Processing Room: 101 ===
Room 101 seats - R: 8, M: 8, L: 0
  Phase 1: Allocating R seats in room 101
  Phase 2: Allocating M seats in room 101
  Phase 3: Allocating L seats in room 101
=== Completed Room: 101 ===

=== Processing Room: 105 ===
Room 105 seats - R: 16, M: 16, L: 16
  Phase 1: Allocating R seats in room 105
  Phase 2: Allocating M seats in room 105
  Phase 3: Allocating L seats in room 105
=== Completed Room: 105 ===

Seating allocation complete: 90 students allocated
Subject distribution: {Commerce-VI=50, India in World Politics=40}
```

**Red Flags:**
- **R and L having the same subject on any bench** (violates R≠L constraint)
- **M having a subject different from both R and L** (violates M=R or M=L rule)
- **Rooms processed out of order** (e.g., Room 101 before Room 11)
- **M seats showing "No seats" when they exist in database** (Phase 2 allocation failing)
- **Large number of unallocated students when seats are available** (logic error)

**Correct Pattern Example (Room 11):**
```
Right Seats (R):          Middle Seats (M):         Left Seats (L):
R1  = AF23001  (Comm-VI)  (no M seats in Room 11)   L1  = BA21131  (India in WP)
R2  = BA23132  (Comm-VI)                            L2  = BA22110  (India in WP)
R3  = BC20013  (Comm-VI)                            L3  = BA23007  (India in WP)
...                                                  ...
R11 = BC21408  (Comm-VI)                            L11 = BA23038  (India in WP)

✓ All R seats in series
✓ All L seats in series
✓ R ≠ L (different subjects)
```

**Correct Pattern Example (Room 101):**
```
Right Seats (R):          Middle Seats (M):         Left Seats (L):
R1  = BC21409  (Comm-VI)  M1 = BC21415  (Comm-VI)   (no L seats in Room 101)
R2  = BC21410  (Comm-VI)  M2 = BC21416  (Comm-VI)
R3  = BC21411  (Comm-VI)  M3 = BC21417  (Comm-VI)
...                       ...
R8  = BC21414  (Comm-VI)  M8 = BC21422  (Comm-VI)

✓ All R seats in series
✓ All M seats in series
✓ M matches R (both Commerce-VI)
✓ Commerce-VI continues from Room 11 (seamless)
```

**Incorrect Pattern Example:**
```
Room 11:
  Right: R9, R10, R11...    ✗ (Should start at R1)
  Middle: No seats          ✗ (If DB has M seats, they should be allocated)
  Left: L1, L2, L3...       ✓ (Correct)
```

## Recent Changes

### January 2026 - Complete Algorithm Rewrite

**Implementation of 5 Core Rules:**

The seating allocation algorithm was completely rewritten to implement these exact requirements:

1. ✅ **L and R have different subjects** - R and L positions always have different subjects on the same bench
2. ✅ **M is continuation of either L or R** - M matches whichever (R or L) has more students available
3. ✅ **Sequence is maintained** - Rooms processed in ID order, seats allocated R1→R2→R3, M1→M2→M3, L1→L2→L3
4. ✅ **R, M, L of room completed before moving to next room** - Room-by-room processing (Room 11 R→M→L complete, then Room 101 R→M→L)
5. ✅ **Subject sequence continues from room to room** - R and L subjects maintain across room boundaries seamlessly

**Major Algorithm Changes - Room-by-Room 2-Subject-Per-Bench Strategy:**

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

- **Fixed**: Room processing order
  - **Old issue**: Rooms sorted by room number (String), causing "101" to come before "11" (alphabetical order)
  - **Solution**: Sort rooms by room ID (Long) instead of room number (String) for proper numeric ordering
  - **Result**: Rooms now processed in correct numeric order: Room 11 → Room 101 → Room 105 → Room 106

- **Fixed**: Allocation order - Changed to room-by-room position-by-position
  - **Old behavior**: Allocated all R seats across ALL rooms, then all M seats, then all L seats
  - **Issue**: Didn't follow rule 4 "R, M, L of the room should be used and then move to next room"
  - **New behavior** (room-by-room):
    - **Room 11**: Phase 1 (all R seats) → Phase 2 (all M seats) → Phase 3 (all L seats)
    - **Room 101**: Phase 1 (all R seats) → Phase 2 (all M seats) → Phase 3 (all L seats)
    - **Room 105**: Phase 1 (all R seats) → Phase 2 (all M seats) → Phase 3 (all L seats)
    - **Room 106**: Phase 1 (all R seats) → Phase 2 (all M seats) → Phase 3 (all L seats)
  - **Result**: Each room completed fully before moving to next room, subject sequence continues across rooms

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
