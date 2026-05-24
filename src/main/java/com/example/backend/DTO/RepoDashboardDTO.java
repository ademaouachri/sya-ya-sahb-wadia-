package com.example.backend.DTO;

import java.math.BigDecimal;
import java.util.Map;

public class RepoDashboardDTO {

    private Long totalClientsGlobal;
    private BigDecimal totalPaidGlobal;
    private BigDecimal totalRemainingGlobal;
    private Long totalClientsWeekly;
    private BigDecimal totalPaidWeekly;

    // 🔥 الزوز كروت الجدد
    private Long totalReportsGlobal;         // عدد التقارير الإجمالي جملة
    private Long totalReportsWeekly;         // عدد التقارير متاع الجمعة هذي

    private Map<String, Long> repartitionParStatut;
    private Map<String, Long> repartitionParPoint;
    private Long activePromessesCount;
    private Long activeFacilitesCount;

    public RepoDashboardDTO() {}

    // الـ Getters والـ Setters الجدد للـ زوز كروت
    public Long getTotalReportsGlobal() { return totalReportsGlobal; }
    public void setTotalReportsGlobal(Long totalReportsGlobal) { this.totalReportsGlobal = totalReportsGlobal; }

    public Long getTotalReportsWeekly() { return totalReportsWeekly; }
    public void setTotalReportsWeekly(Long totalReportsWeekly) { this.totalReportsWeekly = totalReportsWeekly; }

    // ... (خلي الـ Getters والـ Setters القدامى كيف ما هما)
    public Long getTotalClientsGlobal() { return totalClientsGlobal; }
    public void setTotalClientsGlobal(Long totalClientsGlobal) { this.totalClientsGlobal = totalClientsGlobal; }
    public BigDecimal getTotalPaidGlobal() { return totalPaidGlobal; }
    public void setTotalPaidGlobal(BigDecimal totalPaidGlobal) { this.totalPaidGlobal = totalPaidGlobal; }
    public BigDecimal getTotalRemainingGlobal() { return totalRemainingGlobal; }
    public void setTotalRemainingGlobal(BigDecimal totalRemainingGlobal) { this.totalRemainingGlobal = totalRemainingGlobal; }
    public Long getTotalClientsWeekly() { return totalClientsWeekly; }
    public void setTotalClientsWeekly(Long totalClientsWeekly) { this.totalClientsWeekly = totalClientsWeekly; }
    public BigDecimal getTotalPaidWeekly() { return totalPaidWeekly; }
    public void setTotalPaidWeekly(BigDecimal totalPaidWeekly) { this.totalPaidWeekly = totalPaidWeekly; }
    public Map<String, Long> getRepartitionParStatut() { return repartitionParStatut; }
    public void setRepartitionParStatut(Map<String, Long> repartitionParStatut) { this.repartitionParStatut = repartitionParStatut; }
    public Map<String, Long> getRepartitionParPoint() { return repartitionParPoint; }
    public void setRepartitionParPoint(Map<String, Long> repartitionParPoint) { this.repartitionParPoint = repartitionParPoint; }
    public Long getActivePromessesCount() { return activePromessesCount; }
    public void setActivePromessesCount(Long activePromessesCount) { this.activePromessesCount = activePromessesCount; }
    public Long getActiveFacilitesCount() { return activeFacilitesCount; }
    public void setActiveFacilitesCount(Long activeFacilitesCount) { this.activeFacilitesCount = activeFacilitesCount; }
}