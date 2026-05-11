package com.example.backend.DTO;

import com.example.backend.Model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
public class ReportDTO {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;
    private String cli;
    private String point;
    private Double totalAmount;     // المبلغ الجملي المطللوب (Amount)
    private Double paidAmount;      // قداش خلص في البنك
    private Double remainingAmount;
    private String lastPaymentDate;// قداش مازال يسالوه (Calculated)
    private String engagementDate;
    private long daysDiff;          // +6 (مازال) أو -6 (وخر)
    private PaymentStatus status;
    private String observation;
}
