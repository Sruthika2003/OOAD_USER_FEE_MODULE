package com.smartcampusmng.campusmanager.repository;

import com.smartcampusmng.campusmanager.entity.StudentFee;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {
    List<StudentFee> findByStudentAndStatus(User student, StudentFee.PaymentStatus status);
    List<StudentFee> findByStudentAndSemesterAndAcademicYear(User student, String semester, String academicYear);
    boolean existsByStudentAndFeeType(User student, FeeType feeType);
    boolean existsByStudentAndFeeTypeAndSemesterAndAcademicYear(
        User student, FeeType feeType, String semester, String academicYear);
    List<StudentFee> findByStatus(StudentFee.PaymentStatus status);
    List<StudentFee> findBySemesterAndAcademicYearAndStatus(
        String semester, String academicYear, StudentFee.PaymentStatus status);
} 