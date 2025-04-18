package com.smartcampusmng.campusmanager.service;

import com.smartcampusmng.campusmanager.entity.StudentFee;
import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.entity.FeeType;
import com.smartcampusmng.campusmanager.repository.StudentFeeRepository;
import com.smartcampusmng.campusmanager.repository.FeeTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentFeeService {
    private final StudentFeeRepository studentFeeRepository;
    private final FeeTypeRepository feeTypeRepository;

    @Autowired
    public StudentFeeService(StudentFeeRepository studentFeeRepository, 
                           FeeTypeRepository feeTypeRepository) {
        this.studentFeeRepository = studentFeeRepository;
        this.feeTypeRepository = feeTypeRepository;
    }

    public List<StudentFee> getPendingFeesForStudent(User student) {
        return studentFeeRepository.findByStudentAndStatus(
            student, StudentFee.PaymentStatus.PENDING);
    }

    public List<StudentFee> getFeesForStudentAndSemester(
        User student, String semester, String academicYear) {
        // First, ensure all required fees exist
        ensureStudentFeesExist(student, semester, academicYear);
        
        // Then return the fees for the semester
        return studentFeeRepository.findByStudentAndSemesterAndAcademicYear(
            student, semester, academicYear);
    }

    @Transactional
    public void ensureStudentFeesExist(User student, String semester, String academicYear) {
        List<FeeType> allFeeTypes = feeTypeRepository.findAll();
        LocalDate currentDate = LocalDate.now();
        
        for (FeeType feeType : allFeeTypes) {
            // Check if this fee type should be created for the current semester/year
            boolean shouldCreateFee = false;
            String feeSemester = semester;
            String feeAcademicYear = academicYear;

            switch (feeType.getFrequency()) {
                case SEMESTER:
                    // For semester fees, check if we're in the correct semester
                    if (semester.contains("First") && 
                        (currentDate.getMonth() == Month.AUGUST || 
                         currentDate.getMonth() == Month.SEPTEMBER ||
                         currentDate.getMonth() == Month.OCTOBER ||
                         currentDate.getMonth() == Month.NOVEMBER ||
                         currentDate.getMonth() == Month.DECEMBER)) {
                        shouldCreateFee = true;
                    } else if (semester.contains("Second") && 
                        (currentDate.getMonth() == Month.JANUARY || 
                         currentDate.getMonth() == Month.FEBRUARY ||
                         currentDate.getMonth() == Month.MARCH ||
                         currentDate.getMonth() == Month.APRIL ||
                         currentDate.getMonth() == Month.MAY)) {
                        shouldCreateFee = true;
                    }
                    break;
                case YEARLY:
                    // For yearly fees, check if we're in the correct academic year
                    if (academicYear.equals(getCurrentAcademicYear())) {
                        shouldCreateFee = true;
                        feeSemester = null; // Yearly fees don't have a semester
                    }
                    break;
                case ONE_TIME:
                    // For one-time fees, check if they haven't been created yet
                    if (!studentFeeRepository.existsByStudentAndFeeType(student, feeType)) {
                        shouldCreateFee = true;
                        feeSemester = null; // One-time fees don't have a semester
                    }
                    break;
                case MONTHLY:
                    // For monthly fees, create for current month
                    shouldCreateFee = true;
                    feeSemester = semester;
                    break;
            }

            if (shouldCreateFee && !studentFeeRepository.existsByStudentAndFeeTypeAndSemesterAndAcademicYear(
                student, feeType, feeSemester, feeAcademicYear)) {
                createStudentFee(student, feeType, feeSemester, feeAcademicYear);
            }
        }

        // Update status of existing fees to OVERDUE if they cross the due dates
        List<StudentFee> existingFees = studentFeeRepository.findByStudentAndStatus(
            student, StudentFee.PaymentStatus.PENDING);
        
        for (StudentFee fee : existingFees) {
            if (fee.getFeeType().getFrequency() == FeeType.Frequency.SEMESTER) {
                LocalDate dueDate = fee.getDueDate();
                if (dueDate != null) {
                    if (fee.getSemester().contains("First") && currentDate.isAfter(LocalDate.of(currentDate.getYear(), Month.FEBRUARY, 1))) {
                        fee.setStatus(StudentFee.PaymentStatus.OVERDUE);
                        studentFeeRepository.save(fee);
                    } else if (fee.getSemester().contains("Second") && currentDate.isAfter(LocalDate.of(currentDate.getYear(), Month.AUGUST, 1))) {
                        fee.setStatus(StudentFee.PaymentStatus.OVERDUE);
                        studentFeeRepository.save(fee);
                    }
                }
            }
        }
    }

    private void createStudentFee(User student, FeeType feeType, String semester, String academicYear) {
        StudentFee studentFee = new StudentFee();
        studentFee.setStudent(student);
        studentFee.setFeeType(feeType);
        studentFee.setSemester(semester);
        studentFee.setAcademicYear(academicYear);
        studentFee.setAmount(feeType.getAmount());
        
        // Set due date based on fee type
        LocalDate dueDate = LocalDate.now();
        switch (feeType.getFrequency()) {
            case SEMESTER:
                if (semester.contains("First")) {
                    dueDate = LocalDate.of(dueDate.getYear(), Month.OCTOBER, 1);
                } else {
                    dueDate = LocalDate.of(dueDate.getYear(), Month.MARCH, 1);
                }
                break;
            case YEARLY:
                dueDate = LocalDate.of(dueDate.getYear(), Month.JULY, 1);
                break;
            case ONE_TIME:
                dueDate = LocalDate.now().plusDays(30);
                break;
            case MONTHLY:
                dueDate = LocalDate.now().plusDays(15);
                break;
        }
        studentFee.setDueDate(dueDate);
        studentFee.setStatus(StudentFee.PaymentStatus.PENDING);

        studentFeeRepository.save(studentFee);
    }

    private String getCurrentAcademicYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        if (now.getMonthValue() >= 8) { // August or later
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    @Transactional
    public StudentFee updateFeeStatus(StudentFee fee, StudentFee.PaymentStatus status) {
        fee.setStatus(status);
        return studentFeeRepository.save(fee);
    }

    public StudentFee save(StudentFee fee) {
        return studentFeeRepository.save(fee);
    }

    @Transactional
    public void createInitialFeesForStudent(User student) {
        List<FeeType> allFeeTypes = feeTypeRepository.findAll();
        LocalDate currentDate = LocalDate.now();
        String currentAcademicYear = getCurrentAcademicYear();
        String currentSemester = getCurrentSemester();
        
        for (FeeType feeType : allFeeTypes) {
            String feeSemester = null;
            String feeAcademicYear = null;
            boolean shouldCreateFee = false;

            switch (feeType.getFrequency()) {
                case SEMESTER:
                    shouldCreateFee = true;
                    feeSemester = currentSemester;
                    feeAcademicYear = currentAcademicYear;
                    break;
                case YEARLY:
                    shouldCreateFee = true;
                    feeAcademicYear = currentAcademicYear;
                    // For yearly fees, we don't set a semester
                    feeSemester = null;
                    break;
                case ONE_TIME:
                    shouldCreateFee = true;
                    break;
                case MONTHLY:
                    shouldCreateFee = true;
                    feeSemester = currentSemester;
                    feeAcademicYear = currentAcademicYear;
                    break;
            }

            if (shouldCreateFee && !studentFeeRepository.existsByStudentAndFeeTypeAndSemesterAndAcademicYear(
                student, feeType, feeSemester, feeAcademicYear)) {
                createStudentFee(student, feeType, feeSemester, feeAcademicYear);
            }
        }
    }

    private String getCurrentSemester() {
        LocalDate now = LocalDate.now();
        if (now.getMonthValue() >= 8 && now.getMonthValue() <= 12) {
            return "First Semester (Aug-Dec)";
        } else {
            return "Second Semester (Jan-May)";
        }
    }

    public List<StudentFee> getAllPendingFees() {
        return studentFeeRepository.findByStatus(StudentFee.PaymentStatus.PENDING);
    }

    public List<StudentFee> getPendingFeesBySemesterAndYear(String semester, String academicYear) {
        return studentFeeRepository.findBySemesterAndAcademicYearAndStatus(
            semester, academicYear, StudentFee.PaymentStatus.PENDING);
    }
} 