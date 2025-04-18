-- Create database
CREATE DATABASE smart_campus_db;
USE smart_campus_db;
DROP DATABASE smart_campus_db;
DROP TABLE student_fees;
DROP TABLE payments;
-- ===== User Module =====
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('STUDENT', 'FACULTY','ACCOUNTS','ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- ===== Course Management Module =====
CREATE TABLE courses (
    course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    course_description TEXT,
    credit_hours INT NOT NULL,
    faculty_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_id) REFERENCES users(user_id)
);

CREATE TABLE course_materials (
    material_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

CREATE TABLE student_courses (
    student_course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    UNIQUE KEY (student_id, course_id)
);

CREATE TABLE timetable (
    timetable_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- ===== Attendance Module =====
CREATE TABLE attendance (
    attendance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT', 'LATE', 'EXCUSED') NOT NULL DEFAULT 'ABSENT',
    marked_by BIGINT NOT NULL,
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (marked_by) REFERENCES users(user_id),
    UNIQUE KEY (student_id, course_id, attendance_date)
);

CREATE TABLE attendance_reports (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    total_classes INT NOT NULL DEFAULT 0,
    present_count INT NOT NULL DEFAULT 0,
    absent_count INT NOT NULL DEFAULT 0,
    late_count INT NOT NULL DEFAULT 0,
    excused_count INT NOT NULL DEFAULT 0,
    attendance_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    UNIQUE KEY (student_id, course_id, month, year)
);

CREATE TABLE attendance_correction_requests (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attendance_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_comments TEXT,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    FOREIGN KEY (attendance_id) REFERENCES attendance(attendance_id),
    FOREIGN KEY (requested_by) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

-- ===== Exam and Grading System =====
CREATE TABLE exams (
    exam_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_name VARCHAR(100) NOT NULL,
    course_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    exam_type ENUM('MIDTERM', 'FINAL', 'QUIZ', 'ASSIGNMENT') NOT NULL,
    total_marks DECIMAL(5,2) NOT NULL,
    passing_marks DECIMAL(5,2) NOT NULL,
    exam_instructions TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE exam_papers (
    paper_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    FOREIGN KEY (exam_id) REFERENCES exams(exam_id),
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

-- Step 1: Create the table
CREATE TABLE grades (
    grade_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    marks_obtained DECIMAL(5,2) NOT NULL,
    percentage DECIMAL(5,2),  -- Now a regular column
    grade_letter VARCHAR(2),
    feedback TEXT,
    graded_by BIGINT NOT NULL,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (exam_id) REFERENCES exams(exam_id),
    FOREIGN KEY (graded_by) REFERENCES users(user_id),
    UNIQUE KEY (student_id, exam_id)
);

DELIMITER //

CREATE TRIGGER before_insert_grades
BEFORE INSERT ON grades
FOR EACH ROW
BEGIN
    DECLARE total DECIMAL(5,2);
    SELECT total_marks INTO total FROM exams WHERE exam_id = NEW.exam_id;
    SET NEW.percentage = NEW.marks_obtained * 100 / total;
END;
//

CREATE TRIGGER before_update_grades
BEFORE UPDATE ON grades
FOR EACH ROW
BEGIN
    DECLARE total DECIMAL(5,2);
    SELECT total_marks INTO total FROM exams WHERE exam_id = NEW.exam_id;
    SET NEW.percentage = NEW.marks_obtained * 100 / total;
END;
//

DELIMITER ;

-- ===== Payment and Fee Management =====
CREATE TABLE fee_types (
    fee_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fee_name VARCHAR(100) NOT NULL,
    description TEXT,
    amount DECIMAL(10,2) NOT NULL,
    frequency ENUM('ONE_TIME', 'SEMESTER', 'YEARLY', 'MONTHLY') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE student_fees (
    student_fee_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    fee_type_id BIGINT NOT NULL,
    semester VARCHAR(20),
    academic_year VARCHAR(10),
    amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('PENDING', 'PAID','OVERDUE') NOT NULL DEFAULT 'PENDING',
    alerted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (fee_type_id) REFERENCES fee_types(fee_type_id)
);

CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    student_fee_id BIGINT NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'ONLINE_PAYMENT') NOT NULL,
    transaction_reference VARCHAR(100),
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    recorded_by BIGINT NOT NULL,
    remarks TEXT,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (student_fee_id) REFERENCES student_fees(student_fee_id),
    FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

-- Insert sample data for testing purposes
-- Insert sample users
INSERT INTO users (username, password, email, first_name, last_name, role)
VALUES 
('admin1', 'pass123', 'admin@example.com', 'Alice', 'Admin', 'ADMIN'),
('faculty1', 'pass123', 'faculty@example.com', 'Bob', 'Brown', 'FACULTY'),
('student1', 'pass123', 'student1@example.com', 'Charlie', 'Clark', 'STUDENT'),
('student2', 'pass123', 'student2@example.com', 'Dana', 'Doe', 'STUDENT');

INSERT INTO courses (course_code, course_name, course_description, credit_hours, faculty_id)
VALUES 
('CS101', 'Intro to CS', 'Basics of Computer Science', 3, 2);

INSERT INTO timetable (course_id, day_of_week, start_time, end_time, room_number)
VALUES 
(1, 'MONDAY', '10:00:00', '11:00:00', 'A101'),
(1, 'WEDNESDAY', '10:00:00', '11:00:00', 'A101');

INSERT INTO attendance (student_id, course_id, attendance_date, status, marked_by)
VALUES 
(3, 1, '2023-09-04', 'PRESENT', 2),
(3, 1, '2023-09-06', 'ABSENT', 2),
(4, 1, '2023-09-04', 'LATE', 2),
(4, 1, '2023-09-06', 'PRESENT', 2);

INSERT INTO exams (exam_name, course_id, exam_date, start_time, end_time, exam_type, total_marks, passing_marks, created_by)
VALUES 
('Midterm Exam', 1, '2023-10-01', '09:00:00', '11:00:00', 'MIDTERM', 100.00, 40.00, 2);

INSERT INTO grades (student_id, exam_id, marks_obtained, grade_letter, feedback, graded_by)
VALUES 
(3, 1, 85.00, 'A', 'Great job', 2),
(4, 1, 70.00, 'B', 'Good effort', 2);

-- Insert Tuition Fee (id = 1)
INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Tuition Fee', 'Semester-wise tuition fee', 25000.00, 'SEMESTER');

-- Insert Library Fee (id = 2)
INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Library Fee', 'Annual library membership', 2000.00, 'YEARLY');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Bus Fee', 'Semester-wise bus fee', 30000.00, 'SEMESTER');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Other Fee', 'Semester-wise other fee', 10000.00, 'SEMESTER');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Exam Fee', 'Semester-wise tuition fee', 2500.00, 'SEMESTER');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Admission Fee', 'One time Admission fee', 125000.00, 'ONE_TIME');

INSERT INTO student_fees (student_id, fee_type_id, semester, academic_year, amount, due_date)
VALUES 
(3, 1, 'Semester 1', '2023-24', 25000.00, '2023-10-01'),
(3, 2, 'Semester 1', '2023-24', 2000.00, '2023-10-10'),
(4, 1, 'Semester 1', '2023-24', 25000.00, '2023-10-01');

INSERT INTO payments (student_id, student_fee_id, amount, payment_method, receipt_number, recorded_by)
VALUES
(3, 1, 25000.00, 'ONLINE_PAYMENT', 'RCPT001', 1),
(3, 2, 2000.00, 'CASH', 'RCPT002', 1);

SELECT * FROM student_fees WHERE student_fee_id IN (4, 5);


-- Create indexes for better performance
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_courses_faculty ON courses(faculty_id);
CREATE INDEX idx_student_courses_student ON student_courses(student_id);
CREATE INDEX idx_student_courses_course ON student_courses(course_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_course ON attendance(course_id);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_exams_course ON exams(course_id);
CREATE INDEX idx_exams_date ON exams(exam_date);
CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_grades_exam ON grades(exam_id);
CREATE INDEX idx_student_fees_student ON student_fees(student_id);
CREATE INDEX idx_student_fees_status ON student_fees(status);
CREATE INDEX idx_payments_student ON payments(student_id);

-- Create table for tracking fee alerts
CREATE TABLE fee_alerts (
    alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    student_fee_id BIGINT NOT NULL,
    sent_by BIGINT NOT NULL,
    alert_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message TEXT,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (student_fee_id) REFERENCES student_fees(student_fee_id),
    FOREIGN KEY (sent_by) REFERENCES users(user_id),
    UNIQUE KEY (student_id, student_fee_id, sent_by)
);

CREATE INDEX idx_fee_alerts_student ON fee_alerts(student_id);
CREATE INDEX idx_fee_alerts_fee ON fee_alerts(student_fee_id);
CREATE INDEX idx_fee_alerts_sent_by ON fee_alerts(sent_by);

show tables;
delete from fee_types where fee_type_id=5 ;
select * from users ;
select * from fee_types;
