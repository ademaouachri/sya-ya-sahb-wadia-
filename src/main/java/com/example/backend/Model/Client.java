package com.example.backend.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CLIENT")
public class Client {

    @Id
    @Column(name = "CLI", nullable = false)
    private String cli;

    @Column(name = "AGENCY_CODE")
    private String agencyCode;
    @Column(name = "BUSINESS_CENTER_CODE")
    private String businessCenterCode;
    @Column(name = "ACTIVITY_CODE")
    private String activityCode;
    @Column(name = "REGION_CODE")
    private String regionCode;
    @Column(name = "MARCHE_CODE")
    private String marchetCode;
    @Column(name = "SEGMENT_CODE")
    private String segmentCode;
    @Column(name = "ZONE_CODE")
    private String zoneCode;
    @Column(name = "CLASSE")
    private String classe;
    @Column(name = "FULL_NAME")
    private String fullName;
    @Column(name = "CIN")
    private String cin;
    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;
    @Column(name = "TEL")
    private String tel;
    @Column(name = "TEL1")
    private String tel1;
    @Column(name = "TEL2")
    private String tel2;
    @Column(name = "TEL3")
    private String tel3;
    @Column(name = "MAIL")
    private String mail;
    @Column(name = "ADDRESS")
    private String address;
    @Column(name = "ADDRESS1")
    private String address1;
    @Column(name = "ADDRESS2")
    private String address2;
    @Column(name = "POSTAL_CODE")
    private String postalCode;
    @Column(name = "CITY")
    private String city;

    // ====== الحقول المالية الموحدة ======

    @Column(name = "TOTAL_DAYS_IMPAYE")
    private Long totalDaysImpaye;

    @Column(name = "TOTAL_DAYS_SDB")
    private Long totalDaysSdb;

    @Column(name = "TOTAL_IMPAYE_AMOUNT", precision = 20, scale = 3)
    private BigDecimal totalImpayeAmount;

    @Column(name = "TOTAL_DEPASSEMENT", precision = 20, scale = 3)
    private BigDecimal montantDepassement;

    @Column(name = "TOTAL_SDB_AMOUNT", precision = 20, scale = 3)
    private BigDecimal totalSdbAmount;

    // توحيد الاسم إلى engagementGlobal مع ربطه بالعمود total_commitment
    @Column(name = "total_commitment", precision = 20, scale = 3)
    private BigDecimal engagementGlobal;

    @Column(name = "Encours", precision = 20, scale = 3)
    private BigDecimal encours;

    // توحيد الاسم إلى montantAutorise مع ربطه بالعمود total_authorization
    @Column(name = "total_authorization", precision = 20, scale = 3)
    private BigDecimal montantAutorise;

    @Column(name = "sector_commitment", precision = 20, scale = 3)
    private BigDecimal sectorCommitment;

    // ====== باقي الحقول ======
    @Column(name = "is_cloture")
    private String isCloture;
    @Column(name = "CONTACT_FLAG")
    private String contactFlag;
    @Column(name = "TRAITE", length = 1)
    private String traite;
    @Column(name = "CHEQUE_RESTRICTION")
    private String chequeRestriction;
    @Column(name = "SECTOR_CLASS", length = 1)
    private String sectorClass;
    @Column(name = "IS_PARTICULAR", length = 1)
    private String isParticular;
    @Column(name = "ECHEANCE_AUTORISATION")
    private LocalDate echeanceAutorisation;
    @Column(name = "DOSSIER_TYPE")
    private String dossierType;
    @Column(name = "CLIENT_GROUP")
    private String clientGroup;
    @Column(name = "STRUCTURE")
    private String structure;
    @Column(name = "MOTIF_PARTICULAR")
    private String motifParticular;

    // ====== أعمدة التتبع ======
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    @Column(name = "CREATED_BY")
    private String createdBy;
    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) { this.createdAt = LocalDateTime.now(); }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public BigDecimal getMontantTotal() {
        BigDecimal impaye = this.totalImpayeAmount != null ? this.totalImpayeAmount : BigDecimal.ZERO;
        BigDecimal sdb = this.totalSdbAmount != null ? this.totalSdbAmount : BigDecimal.ZERO;
        return impaye.add(sdb);
    }
}