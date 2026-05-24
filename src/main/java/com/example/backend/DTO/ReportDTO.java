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
    private Double totalAmount;     // المبلغ الجملي المطلوب (Amount)
    private Double paidAmount;      // قداش خلص في البنك
    private Double remainingAmount; // قداش مازال يسالوه (Calculated)
    private String lastPaymentDate;
    private String engagementDate;
    private long daysDiff;          // +6 (مازال) أو -6 (وخر)
    private PaymentStatus status;
    private String observation;

    // --- الحقول الجديدة الخاصة بالزيارة ---
    private String visitDate;       // تاريخ الزيارة المبرمج
    private String visitTime;       // وقت الزيارة
    private String location;        // مكان الزيارة (Tunis, Sousse, etc.)
}