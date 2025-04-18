package com.smartcampusmng.campusmanager.service;

import com.smartcampusmng.campusmanager.entity.Payment;
import com.smartcampusmng.campusmanager.entity.StudentFee;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.entity.FeeAlert;
import com.smartcampusmng.campusmanager.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StudentFeeService studentFeeService;
    private final FeeAlertService feeAlertService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, 
                         StudentFeeService studentFeeService,
                         FeeAlertService feeAlertService) {
        this.paymentRepository = paymentRepository;
        this.studentFeeService = studentFeeService;
        this.feeAlertService = feeAlertService;
    }

    @Transactional
    public Payment processPayment(StudentFee fee, User student, User recordedBy, 
                                Payment.PaymentMethod method, String transactionRef) {
        // Create payment record
        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setStudentFee(fee);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmount(fee.getAmount());
        payment.setPaymentMethod(method);
        payment.setTransactionReference(transactionRef);
        payment.setReceiptNumber(generateReceiptNumber());
        payment.setRecordedBy(recordedBy);
        payment.setRemarks("Payment processed for " + fee.getFeeType().getFeeName());

        // Update fee status
        studentFeeService.updateFeeStatus(fee, StudentFee.PaymentStatus.PAID);

        // Remove any existing alerts for this fee
        List<FeeAlert> existingAlerts = feeAlertService.getAlertsForStudentAndFee(student, fee);
        for (FeeAlert alert : existingAlerts) {
            feeAlertService.deleteAlert(alert);
        }

        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsForStudent(User student) {
        return paymentRepository.findByStudent(student);
    }

    public List<Payment> getPaymentsForStudentAndSemester(
        User student, String semester, String academicYear) {
        return paymentRepository.findByStudentAndStudentFee_SemesterAndStudentFee_AcademicYear(
            student, semester, academicYear);
    }

    public List<Payment> getAllPaymentsBySemesterAndYear(String semester, String academicYear) {
        return paymentRepository.findByStudentFee_SemesterAndStudentFee_AcademicYear(
            semester, academicYear);
    }

    private String generateReceiptNumber() {
        return "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
} 