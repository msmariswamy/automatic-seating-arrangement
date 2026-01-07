-- =====================================================
-- Automatic Seating Arrangement System - Database Setup
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS seating_db;

-- Connect to database
\c seating_db;

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS seating_arrangements CASCADE;
DROP TABLE IF EXISTS student_subjects CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin user (password: admin123)
INSERT INTO users (username, password, role, active)
VALUES ('admin', '$2a$10$8.UnVuG9HHgfflqvSr8dKuZ5cxvKFWKhVZQjQqQyQJQnKGQhQJqvi', 'ADMIN', TRUE);

-- =====================================================
-- Students Table
-- =====================================================
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    roll_no VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    is_allocated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roll_no ON students(roll_no);
CREATE INDEX idx_department ON students(department);
CREATE INDEX idx_class ON students(class_name);

-- =====================================================
-- Student Subjects Table (Many-to-Many relationship)
-- =====================================================
CREATE TABLE student_subjects (
    student_id BIGINT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, subject)
);

-- =====================================================
-- Rooms Table
-- =====================================================
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    room_no VARCHAR(50) UNIQUE NOT NULL,
    total_benches INTEGER NOT NULL CHECK (total_benches > 0),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    r_count INTEGER NOT NULL CHECK (r_count >= 0),
    m_count INTEGER NOT NULL CHECK (m_count >= 0),
    l_count INTEGER NOT NULL CHECK (l_count >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_capacity CHECK (capacity = r_count + m_count + l_count)
);

CREATE INDEX idx_room_no ON rooms(room_no);

-- =====================================================
-- Seats Table
-- =====================================================
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    seat_no VARCHAR(10) NOT NULL,
    position VARCHAR(1) NOT NULL CHECK (position IN ('R', 'M', 'L')),
    bench_no INTEGER NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT unique_seat UNIQUE (room_id, seat_no)
);

CREATE INDEX idx_room_seat ON seats(room_id, seat_no);
CREATE INDEX idx_bench_position ON seats(bench_no, position);

-- =====================================================
-- Seating Arrangements Table
-- =====================================================
CREATE TABLE seating_arrangements (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    arrangement_date DATE NOT NULL,
    arrangement_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_arrangement_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_arrangement_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_arrangement_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE
);

CREATE INDEX idx_arrangement_date ON seating_arrangements(arrangement_date);
CREATE INDEX idx_room_arrangement ON seating_arrangements(room_id, arrangement_date);
CREATE INDEX idx_student_arrangement ON seating_arrangements(student_id);

-- =====================================================
-- Sample Data (Optional - for testing)
-- =====================================================

-- Sample Students
INSERT INTO students (roll_no, name, department, class_name, is_allocated) VALUES
('CS001', 'John Doe', 'CSE', 'BE-IV', FALSE),
('CS002', 'Jane Smith', 'CSE', 'BE-IV', FALSE),
('CS003', 'Mike Johnson', 'CSE', 'BE-IV', FALSE),
('EC001', 'Sarah Wilson', 'ECE', 'BE-IV', FALSE),
('EC002', 'Tom Brown', 'ECE', 'BE-IV', FALSE),
('ME001', 'Emma Davis', 'MECH', 'BE-IV', FALSE),
('ME002', 'Oliver Miller', 'MECH', 'BE-IV', FALSE);

-- Sample Student Subjects
INSERT INTO student_subjects (student_id, subject) VALUES
(1, 'Mathematics'),
(1, 'Data Structures'),
(2, 'Physics'),
(2, 'Algorithms'),
(3, 'Mathematics'),
(3, 'Operating Systems'),
(4, 'Physics'),
(4, 'Digital Electronics'),
(5, 'Mathematics'),
(5, 'Control Systems'),
(6, 'Physics'),
(6, 'Thermodynamics'),
(7, 'Mathematics'),
(7, 'Fluid Mechanics');

-- Sample Rooms
INSERT INTO rooms (room_no, total_benches, capacity, r_count, m_count, l_count) VALUES
('101', 10, 30, 10, 10, 10),
('102', 10, 30, 10, 10, 10),
('103', 15, 45, 15, 15, 15);

-- Sample Seats for Room 101
DO $$
DECLARE
    room_id BIGINT;
    bench INT;
BEGIN
    SELECT id INTO room_id FROM rooms WHERE room_no = '101';

    FOR bench IN 1..10 LOOP
        INSERT INTO seats (room_id, seat_no, position, bench_no, is_occupied) VALUES
        (room_id, 'R' || bench, 'R', bench, FALSE),
        (room_id, 'M' || bench, 'M', bench, FALSE),
        (room_id, 'L' || bench, 'L', bench, FALSE);
    END LOOP;
END $$;

-- =====================================================
-- Useful Queries
-- =====================================================

-- View all students with their subjects
-- SELECT s.roll_no, s.name, s.department, s.class_name,
--        string_agg(ss.subject, ', ') as subjects
-- FROM students s
-- LEFT JOIN student_subjects ss ON s.id = ss.student_id
-- GROUP BY s.id, s.roll_no, s.name, s.department, s.class_name;

-- View room capacity summary
-- SELECT room_no, total_benches, capacity,
--        (SELECT COUNT(*) FROM seats WHERE room_id = r.id) as total_seats,
--        (SELECT COUNT(*) FROM seats WHERE room_id = r.id AND is_occupied = TRUE) as occupied_seats
-- FROM rooms r;

-- View seating arrangements for a specific date
-- SELECT sa.arrangement_date, r.room_no, s.seat_no, st.roll_no, st.name, sa.subject
-- FROM seating_arrangements sa
-- JOIN students st ON sa.student_id = st.id
-- JOIN rooms r ON sa.room_id = r.id
-- JOIN seats s ON sa.seat_id = s.id
-- WHERE sa.arrangement_date = CURRENT_DATE
-- ORDER BY r.room_no, s.bench_no, s.position;

-- =====================================================
-- End of Script
-- =====================================================
