package com.smartcampusmng.campusmanager.repository;

import com.smartcampusmng.campusmanager.entity.FeeAlert;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.entity.StudentFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeAlertRepository extends JpaRepository<FeeAlert, Long> {
    List<FeeAlert> findByStudentAndStudentFee(User student, StudentFee studentFee);
    boolean existsByStudentAndStudentFeeAndSentBy(User student, StudentFee studentFee, User sentBy);
    List<FeeAlert> findBySentBy(User sentBy);
    List<FeeAlert> findByStudent(User student);
} 