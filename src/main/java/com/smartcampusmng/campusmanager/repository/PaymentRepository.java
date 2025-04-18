package com.smartcampusmng.campusmanager.repository;

import com.smartcampusmng.campusmanager.entity.Payment;
import com.smartcampusmng.campusmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStudent(User student);
    List<Payment> findByStudentAndStudentFee_SemesterAndStudentFee_AcademicYear(
        User student, String semester, String academicYear);
    List<Payment> findByStudentFee_SemesterAndStudentFee_AcademicYear(
        String semester, String academicYear);
} 