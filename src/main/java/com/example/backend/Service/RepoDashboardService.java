package com.example.backend.Service;

import com.example.backend.DTO.RepoDashboardDTO;
import com.example.backend.Model.Utilisateur;
import com.example.backend.Repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RepoDashboardService {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public RepoDashboardDTO getRecouvreurStats(Utilisateur utilisateur, String point) { // 🔥 زدنا String point هوني باش يتصلح الكومبيل
        RepoDashboardDTO dto = new RepoDashboardDTO();
        String matricule = utilisateur.getMatricule();

        // تنظيف الـ String إذا كان فارغ جاي م الـ URL أو م الـ PDF
        String filterPoint = (point != null && !point.trim().isEmpty()) ? point.trim() : null;

        // ---------------------------------------------------------
        // 1. 📈 الحسبة الإجمالية (Global Stats) م الـ Report
        // ---------------------------------------------------------
        Object globalData = reportRepository.getGlobalReportStats(matricule, filterPoint);
        double totalAmount = 0.0;
        double totalPaid = 0.0;

        if (globalData != null) {
            Object[] row = (Object[]) globalData;

            // أ) عدد العملاء الفريدين إجمالياً (DISTINCT cli)
            dto.setTotalClientsGlobal(row[0] != null ? (Long) row[0] : 0L);

            // ب) إجمالي الفلوس اللي تخلصت (Paid Amount) ملي بدأ
            totalAmount = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            totalPaid = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            dto.setTotalPaidGlobal(BigDecimal.valueOf(totalPaid));

            // ج) قداش مازالوا يسالوا فلوس (Remaining = Amount - Paid)
            double remaining = totalAmount - totalPaid;
            dto.setTotalRemainingGlobal(BigDecimal.valueOf(remaining >= 0 ? remaining : 0.0));
        } else {
            dto.setTotalClientsGlobal(0L);
            dto.setTotalPaidGlobal(BigDecimal.ZERO);
            dto.setTotalRemainingGlobal(BigDecimal.ZERO);
        }

        // د) إجمالي عدد التقارير المكتوبة ملي بدأ
        dto.setTotalReportsGlobal(reportRepository.countTotalReportsGlobal(matricule, filterPoint));

        // ---------------------------------------------------------
        // 2. 📅 الحسبة الأسبوعية (Weekly Stats) - من الإثنين للـ اليوم
        // ---------------------------------------------------------
        LocalDateTime startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime endOfWeek = LocalDateTime.now();

        // أ) عدد العملاء الفريدين في الجمعة هذي
        Long weeklyClients = reportRepository.countWeeklyClients(matricule, startOfWeek, endOfWeek, filterPoint);
        dto.setTotalClientsWeekly(weeklyClients != null ? weeklyClients : 0L);

        // ب) إجمالي الفلوس اللي تلمت في الجمعة هذي بالذات
        Double weeklyPaid = reportRepository.sumWeeklyPaidAmount(matricule, startOfWeek, endOfWeek, filterPoint);
        dto.setTotalPaidWeekly(BigDecimal.valueOf(weeklyPaid != null ? weeklyPaid : 0.0));

        // ج) عدد التقارير المكتوبة في الجمعة هذي بالذات
        Long weeklyReports = reportRepository.countTotalReportsWeekly(matricule, startOfWeek, endOfWeek, filterPoint);
        dto.setTotalReportsWeekly(weeklyReports != null ? weeklyReports : 0L);

        // ---------------------------------------------------------
        // 3. 📊 توزيع الحالات (الـ Donut Chart)
        // ---------------------------------------------------------
        List<Object[]> statusResults = reportRepository.countStatusForRecouvreur(matricule, filterPoint);
        Map<String, Long> statutMap = new HashMap<>();
        for (Object[] row : statusResults) {
            if (row[0] != null) {
                statutMap.put(row[0].toString(), (Long) row[1]);
            }
        }
        dto.setRepartitionParStatut(statutMap);

        // ---------------------------------------------------------
        // 4. 📉 توزيع الإجراءات (الـ Progress Bars)
        // ---------------------------------------------------------
        List<Object[]> pointResults = reportRepository.countPointsForRecouvreur(matricule);
        Map<String, Long> pointMap = new HashMap<>();
        for (Object[] row : pointResults) {
            if (row[0] != null) {
                String label = decodePointCode(row[0].toString());
                pointMap.put(label, (Long) row[1]);
            }
        }
        dto.setRepartitionParPoint(pointMap);

        // ---------------------------------------------------------
        // 5. 🔔 العدادات والتنبيهات النشطة
        // ---------------------------------------------------------
        dto.setActivePromessesCount(reportRepository.countActivePromessesForRecouvreur(matricule));
        dto.setActiveFacilitesCount(reportRepository.countActiveFacilitesForRecouvreur(matricule));

        return dto;
    }

    /**
     * ميثود مساعدة لتحويل أكواد الـ Point لتسميات مفهومة في الـ Front
     */
    private String decodePointCode(String code) {
        switch (code) {
            case "P001": return "Visite Terrain";
            case "P002": return "Promesse de règlement";
            case "P003": return "Facilité de paiement";
            case "P004": return "Client injoignable";
            default: return code;
        }
    }
}