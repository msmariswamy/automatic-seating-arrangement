# Automatic Seating Arrangement System

A complete web-based application for managing and generating seating arrangements with constraint-based allocation algorithm. Built with Spring Boot, PostgreSQL, and modern web technologies.

## Features

### 1. Admin Authentication
- Secure login system with Spring Security
- Session management
- Password encryption using BCrypt

### 2. Student Management
- Download Excel template with predefined format
- Bulk upload student data via Excel
- Support for multiple subjects per student
- Department, class, and subject-wise filtering

### 3. Room Management
- Download Excel template for room configuration
- Bulk upload room data with seat specifications
- Support for R (Right), M (Middle), L (Left) seat positions
- Automatic seat generation based on bench count

### 4. Seating Arrangement Generation
- **Core Algorithm Features:**
  - No two students with the same subject on the same bench
  - Optimal room utilization
  - Even distribution across available rooms
  - Multi-criteria filtering (Department, Class, Subject)

### 5. Reporting System
- **Consolidated Report:** Department-wise summary with seat ranges
- **Individual Room Reports:** Detailed seat allocation for each room
- **PDF Export:** Download room-specific reports as PDF
- Historical arrangement tracking by date

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.1
- **Security:** Spring Security with BCrypt
- **Database:** PostgreSQL 15+
- **ORM:** Spring Data JPA, Hibernate
- **Excel Processing:** Apache POI 5.2.5
- **PDF Generation:** iText 5.5.13
- **Frontend:** HTML5, CSS3, Bootstrap 5, jQuery 3.7
- **Templating:** Thymeleaf
- **Build Tool:** Gradle 8.5

## Prerequisites

- Java 17 or higher
- PostgreSQL 15 or higher
- Gradle 8.5+ (or use included wrapper)
- Git (optional)

## Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd automatic-seating-arrangement
```

### 2. Database Setup

#### Option A: Using the provided SQL script

```bash
# Create database and tables
psql -U postgres -f database/init.sql
```

#### Option B: Manual setup

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE seating_db;

-- Exit and run the init script
\q
psql -U postgres -d seating_db -f database/init.sql
```

### 3. Configure Application Properties

Edit `src/main/resources/application.properties` if needed:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/seating_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# Change server port if needed
server.port=8080
```

### 4. Build the Application

```bash
# Using Gradle wrapper (recommended - no Gradle installation needed)
./gradlew clean build

# Or if you have Gradle installed
gradle clean build
```

### 5. Run the Application

```bash
# Using Gradle wrapper (recommended)
./gradlew bootRun

# Or if you have Gradle installed
gradle bootRun
```

Or run the JAR file:

```bash
java -jar build/libs/automatic-seating-arrangement-1.0.0.jar
```

### 6. Access the Application

Open your browser and navigate to:
```
http://localhost:8080
```

**Default Credentials:**
- Username: `admin`
- Password: `admin123`

## Usage Guide

### Step 1: Upload Student Data

1. Navigate to the Dashboard
2. In the **Student Management** section:
   - Click "Download Excel Template"
   - Fill in student details (Roll No, Name, Department, Class, Subjects)
   - Upload the completed Excel file
3. Verify student count in statistics

### Step 2: Upload Room Data

1. In the **Room Management** section:
   - Click "Download Excel Template"
   - Fill in room details (Room No, Total Benches, Capacity, R/M/L counts)
   - Upload the completed Excel file
2. Verify room and seat counts in statistics

### Step 3: Generate Seating Arrangement

1. Scroll to **Generate Seating Arrangement** section
2. Select criteria:
   - Check desired departments
   - Check desired classes
   - Check desired subjects
3. (Optional) Enter an arrangement name
4. Click "Generate Seating Arrangement"
5. System will allocate seats following the constraint rules

### Step 4: View Reports

1. Click "View Reports" in the navigation
2. Select arrangement date from dropdown
3. **Consolidated Report Tab:**
   - View department-wise summary
   - See seat ranges and total counts per room
4. **Individual Room Reports Tab:**
   - View detailed seat allocation
   - See three columns (Right, Middle, Left seats)
   - Download PDF for any room

## Excel Template Formats

### Student Template

| Roll No | Student Name | Department | Class | Subject1 | Subject2 | Subject3 | Subject4 | Subject5 |
|---------|--------------|------------|-------|----------|----------|----------|----------|----------|
| CS001   | John Doe     | CSE        | BE-IV | Mathematics | Physics | | | |

**Rules:**
- Roll No must be unique
- All fields except subjects are mandatory
- Leave subject columns empty if not applicable
- Multiple subjects per student are supported

### Room Template

| Room No | Total Benches | Capacity | R Count | M Count | L Count |
|---------|---------------|----------|---------|---------|---------|
| 101     | 30            | 90       | 30      | 30      | 30      |

**Rules:**
- Room No must be unique
- Capacity = R Count + M Count + L Count
- All fields are mandatory
- Seats are auto-generated as: R1, M1, L1, R2, M2, L2, ...

## Seating Algorithm Explained

The system uses a constraint-based allocation algorithm:

1. **Group students by subject**
2. **Iterate through benches in each room**
3. **For each bench (R, M, L positions):**
   - Assign students with different subjects
   - Track used subjects per bench
   - Prevent same-subject conflicts
4. **Optimize room utilization**
5. **Mark students and seats as allocated**

### Constraints:
- ✅ No two students with the same subject on the same bench
- ✅ Each position (R, M, L) on a bench has different subjects
- ✅ Students are evenly distributed across rooms
- ✅ Room capacity is respected

## API Endpoints

### Student Management
- `GET /api/students/template` - Download student template
- `POST /api/students/upload` - Upload student data
- `GET /api/students` - Get all students
- `GET /api/students/departments` - Get all departments
- `GET /api/students/classes` - Get all classes
- `GET /api/students/subjects` - Get all subjects
- `DELETE /api/students` - Delete all students

### Room Management
- `GET /api/rooms/template` - Download room template
- `POST /api/rooms/upload` - Upload room data
- `GET /api/rooms` - Get all rooms
- `DELETE /api/rooms` - Delete all rooms

### Seating Arrangement
- `POST /api/seating/generate` - Generate seating arrangement
- `GET /api/seating/reports/consolidated` - Get consolidated report
- `GET /api/seating/reports/rooms` - Get room reports
- `GET /api/seating/reports/room/pdf` - Download room PDF
- `GET /api/seating/dates` - Get all arrangement dates
- `DELETE /api/seating` - Delete arrangement

## Project Structure

```
automatic-seating-arrangement/
├── src/
│   ├── main/
│   │   ├── java/com/seating/
│   │   │   ├── config/              # Security configuration
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── repository/          # Spring Data repositories
│   │   │   ├── service/             # Business logic
│   │   │   └── SeatingArrangementApplication.java
│   │   └── resources/
│   │       ├── templates/           # Thymeleaf HTML templates
│   │       └── application.properties
│   └── test/                        # Test classes
├── database/
│   └── init.sql                     # Database initialization script
├── build.gradle                     # Gradle build configuration
├── settings.gradle                  # Gradle settings
├── gradle/
│   └── wrapper/                     # Gradle wrapper files
├── gradlew                          # Gradle wrapper script (Linux/Mac)
├── gradlew.bat                      # Gradle wrapper script (Windows)
└── README.md                        # This file
```

## Database Schema

### Tables:
- **users** - Admin authentication
- **students** - Student information
- **student_subjects** - Student-subject mapping
- **rooms** - Room configuration
- **seats** - Individual seats in rooms
- **seating_arrangements** - Generated arrangements

### Key Relationships:
- Student ↔ Subjects (Many-to-Many)
- Room → Seats (One-to-Many)
- SeatingArrangement → Student, Room, Seat (Many-to-One)

## Troubleshooting

### Database Connection Issues
```bash
# Verify PostgreSQL is running
sudo systemctl status postgresql

# Check database exists
psql -U postgres -l | grep seating_db

# Test connection
psql -U postgres -d seating_db -c "SELECT version();"
```

### Port Already in Use
```bash
# Change port in application.properties
server.port=8081
```

### Excel Upload Fails
- Ensure file format is .xlsx or .xls
- Check template headers match exactly
- Verify no special characters in data
- Check file size (max 10MB)

### Seating Generation Returns No Results
- Verify students and rooms exist in database
- Check filter criteria matches student data
- Ensure rooms have available seats
- Review application logs for detailed errors

## Future Enhancements

- [ ] Multiple exam session management
- [ ] Student photo integration
- [ ] Invigilator assignment
- [ ] Email notifications
- [ ] Mobile responsive design improvements
- [ ] Advanced reporting with charts
- [ ] Export to multiple formats (CSV, Word)
- [ ] Barcode/QR code generation for seats
- [ ] Real-time collaboration features

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions:
- Create an issue on GitHub
- Contact: your-email@example.com

## Acknowledgments

- Spring Boot Framework
- Apache POI Library
- iText PDF Library
- Bootstrap CSS Framework

---

**Built with ❤️ for educational institutions**
