package com.smartcampusmng.campusmanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_alerts")
@Data
public class FeeAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "student_fee_id", nullable = false)
    private StudentFee studentFee;

    @ManyToOne
    @JoinColumn(name = "sent_by", nullable = false)
    private User sentBy;

    @Column(name = "alert_date", nullable = false)
    private LocalDateTime alertDate;

    @Column(name = "message", nullable = false)
    private String message;
} 