package com.example.backend.DTO;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DashboardStats {
    private Long totalDossiers;
    private BigDecimal totalImpayes;
    private BigDecimal totalSdb;

    // الحقول الجديدة اللي حاشتك بيها
    private BigDecimal totalAutorise;
    private BigDecimal totalEncours;
    private BigDecimal totalEngagement;
    private BigDecimal totalDepassement;

    private List<MonthlyEvolutionDTO> monthlyEvolution;

    // Constructor الجديد مطابق لـ getGlobalFinancialStats و getDashboardGlobalStats
    public DashboardStats(Long totalDossiers, Object totalImpayes, Object totalSdb,
                          Object totalEngagement, Object totalAutorise,
                          Object totalEncours, Object totalDepassement) {

        this.totalDossiers = totalDossiers;
        this.totalImpayes = toBigDecimal(totalImpayes);
        this.totalSdb = toBigDecimal(totalSdb);
        this.totalEngagement = toBigDecimal(totalEngagement);
        this.totalAutorise = toBigDecimal(totalAutorise);
        this.totalEncours = toBigDecimal(totalEncours);
        this.totalDepassement = toBigDecimal(totalDepassement);
    }

    // ميثود مساعدة باش ما نكرروش كود التحويل في كل بلاصة
    private BigDecimal toBigDecimal(Object value) {
        return (value instanceof Number) ? BigDecimal.valueOf(((Number) value).doubleValue()) : BigDecimal.ZERO;
    }
}