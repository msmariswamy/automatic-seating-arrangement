# Quick Setup Guide

## Prerequisites Checklist

- [ ] Java 17 or higher installed
- [ ] PostgreSQL 15+ installed and running
- [ ] Gradle 8.5+ installed (optional - wrapper included)
- [ ] Git installed (optional)

## Step-by-Step Setup

### 1. Install Java 17

#### Windows:
```bash
# Download from Oracle or use Chocolatey
choco install openjdk17
```

#### macOS:
```bash
# Using Homebrew
brew install openjdk@17
```

#### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

Verify installation:
```bash
java -version
```

### 2. Install PostgreSQL

#### Windows:
Download installer from https://www.postgresql.org/download/windows/

#### macOS:
```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

Verify installation:
```bash
psql --version
```

### 3. Setup Database

```bash
# Switch to postgres user (Linux/Mac)
sudo -u postgres psql

# Or directly connect (Windows/Mac with Homebrew)
psql -U postgres

# Run the following SQL commands:
CREATE DATABASE seating_db;
\q
```

Run the initialization script:
```bash
psql -U postgres -d seating_db -f database/init.sql
```

### 4. Configure Application

Edit `src/main/resources/application.properties`:

```properties
# Update these if your PostgreSQL has different credentials
spring.datasource.username=postgres
spring.datasource.password=your_password_here
```

### 5. Build and Run

```bash
# Using Gradle wrapper (recommended - no Gradle installation needed)
./gradlew clean build

# Run the application
./gradlew bootRun
```

**For Windows, use `gradlew.bat` instead of `./gradlew`**

Alternative - Run as JAR:
```bash
java -jar build/libs/automatic-seating-arrangement-1.0.0.jar
```

### 6. Access Application

Open browser: http://localhost:8080

Login credentials:
- Username: `admin`
- Password: `admin123`

## Common Setup Issues

### Issue: Port 8080 already in use

**Solution:** Change port in `application.properties`
```properties
server.port=8081
```

### Issue: Database connection refused

**Solution:**
1. Check PostgreSQL is running:
   ```bash
   sudo systemctl status postgresql
   ```
2. Verify database exists:
   ```bash
   psql -U postgres -l | grep seating_db
   ```
3. Check credentials in application.properties

### Issue: Gradle build fails

**Solution:**
1. Verify Java version:
   ```bash
   java -version
   ```
2. Clear Gradle cache and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```
3. If issues persist, delete Gradle cache:
   ```bash
   rm -rf ~/.gradle/caches
   ./gradlew clean build
   ```

### Issue: Excel upload fails

**Solution:**
1. Ensure file is .xlsx format
2. Check file size (max 10MB)
3. Verify template headers match exactly
4. Check for special characters in data

## Testing the Application

### 1. Test with Sample Data

The database initialization script includes sample data. After setup:

1. Login to the application
2. Check statistics dashboard (should show 7 students, 3 rooms)
3. Try generating a seating arrangement with sample data

### 2. Test Excel Upload

#### Create Test Student File:

| Roll No | Student Name | Department | Class | Subject1 | Subject2 | Subject3 |
|---------|--------------|------------|-------|----------|----------|----------|
| TEST001 | Test Student | CSE        | BE-IV | Math     | Physics  |          |

Save as `test_students.xlsx` and upload.

#### Create Test Room File:

| Room No | Total Benches | Capacity | R Count | M Count | L Count |
|---------|---------------|----------|---------|---------|---------|
| TEST101 | 5             | 15       | 5       | 5       | 5       |

Save as `test_rooms.xlsx` and upload.

### 3. Generate Test Arrangement

1. Select all departments
2. Select all classes
3. Select all subjects
4. Click "Generate Seating Arrangement"
5. Navigate to Reports to view results

## Development Setup

### Using IntelliJ IDEA

1. File → Open → Select project folder
2. Wait for Gradle import (automatic)
3. Configure JDK: File → Project Structure → Project SDK → Java 17
4. Run configuration:
   - Use Gradle task: `bootRun`
   - Or main class: `com.seating.SeatingArrangementApplication`

### Using Eclipse

1. File → Import → Gradle → Existing Gradle Project
2. Select project folder
3. Wait for Gradle dependencies to download
4. Right-click project → Run As → Spring Boot App

### Using VS Code

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
2. Open project folder
3. Press F5 to run

## Database Management

### View Database Content

```sql
-- Connect to database
psql -U postgres -d seating_db

-- View all students
SELECT * FROM students;

-- View all rooms
SELECT * FROM rooms;

-- View seating arrangements
SELECT sa.*, s.roll_no, s.name, r.room_no, seat.seat_no
FROM seating_arrangements sa
JOIN students s ON sa.student_id = s.id
JOIN rooms r ON sa.room_id = r.id
JOIN seats seat ON sa.seat_id = seat.id;
```

### Reset Database

```sql
-- Delete all data (keeps structure)
DELETE FROM seating_arrangements;
DELETE FROM student_subjects;
DELETE FROM students;
DELETE FROM seats;
DELETE FROM rooms;

-- Or drop and recreate database
DROP DATABASE seating_db;
CREATE DATABASE seating_db;
```

Then run init.sql again.

## Production Deployment

### 1. Build for Production

```bash
# Build without running tests
./gradlew clean build -x test

# Or build with tests
./gradlew clean build
```

### 2. Configure Production Settings

Create `application-prod.properties`:
```properties
spring.datasource.url=jdbc:postgresql://production-host:5432/seating_db
spring.datasource.username=prod_user
spring.datasource.password=secure_password
spring.jpa.hibernate.ddl-auto=validate
logging.level.root=WARN
```

### 3. Run with Production Profile

```bash
java -jar build/libs/automatic-seating-arrangement-1.0.0.jar --spring.profiles.active=prod
```

### 4. Using Docker (Optional)

Create `Dockerfile`:
```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
docker build -t seating-app .
docker run -p 8080:8080 seating-app
```

## Maintenance

### Backup Database

```bash
pg_dump -U postgres seating_db > backup_$(date +%Y%m%d).sql
```

### Restore Database

```bash
psql -U postgres seating_db < backup_20240101.sql
```

### Monitor Application Logs

```bash
tail -f logs/spring.log
```

### Update Application

```bash
git pull origin main
./gradlew clean build
# Restart application
```

## Getting Help

If you encounter issues:

1. Check application logs: `logs/spring.log`
2. Check PostgreSQL logs: `/var/log/postgresql/`
3. Verify all prerequisites are installed
4. Review error messages carefully
5. Check README.md for detailed documentation

## Next Steps

After successful setup:

1. ✅ Change default admin password
2. ✅ Upload your student data
3. ✅ Upload your room data
4. ✅ Generate seating arrangements
5. ✅ Export reports as needed
6. ✅ Backup database regularly

---

**Happy arranging! 🎓**
