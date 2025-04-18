package com.smartcampusmng.campusmanager.service;

import com.smartcampusmng.campusmanager.entity.FeeAlert;
import com.smartcampusmng.campusmanager.entity.StudentFee;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.repository.FeeAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeeAlertService {
    private final FeeAlertRepository feeAlertRepository;
    private final StudentFeeService studentFeeService;

    @Autowired
    public FeeAlertService(FeeAlertRepository feeAlertRepository, StudentFeeService studentFeeService) {
        this.feeAlertRepository = feeAlertRepository;
        this.studentFeeService = studentFeeService;
    }

    @Transactional
    public FeeAlert createAlert(User student, StudentFee studentFee, User sentBy, String message) {
        // Check if alert already exists
        if (feeAlertRepository.existsByStudentAndStudentFeeAndSentBy(student, studentFee, sentBy)) {
            throw new RuntimeException("Alert already sent to this student for this fee");
        }

        // Mark the fee as alerted
        if (studentFee.getStatus() == StudentFee.PaymentStatus.PENDING) {
            studentFee.setAlerted(true);
            studentFeeService.save(studentFee);
        }

        FeeAlert alert = new FeeAlert();
        alert.setStudent(student);
        alert.setStudentFee(studentFee);
        alert.setSentBy(sentBy);
        alert.setAlertDate(LocalDateTime.now());
        alert.setMessage(message);

        return feeAlertRepository.save(alert);
    }

    public List<FeeAlert> getAlertsForStudentAndFee(User student, StudentFee studentFee) {
        return feeAlertRepository.findByStudentAndStudentFee(student, studentFee);
    }

    public boolean hasAlertForFee(StudentFee studentFee, User sentBy) {
        return feeAlertRepository.existsByStudentAndStudentFeeAndSentBy(
            studentFee.getStudent(), studentFee, sentBy);
    }

    public List<FeeAlert> getAlertsBySentBy(User sentBy) {
        return feeAlertRepository.findBySentBy(sentBy);
    }

    public List<FeeAlert> getAlertsForStudent(User student) {
        return feeAlertRepository.findByStudent(student);
    }

    @Transactional
    public void deleteAlert(FeeAlert alert) {
        feeAlertRepository.delete(alert);
    }
} 