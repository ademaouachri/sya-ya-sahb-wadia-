package com.example.backend.Service;

import com.example.backend.DTO.ReportDTO;
import com.example.backend.Model.PaymentStatus;
import com.example.backend.Model.Report;
import com.example.backend.Repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * جلب كافة التقارير وتحويلها لـ DTO مع التزامن مع البنك
     */
    public List<ReportDTO> getAllReportsWithAutoCheck() {
        List<Report> reports = reportRepository.findAll();
        return reports.stream()
                .map(this::processAndConvertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * تحويل الـ Entity إلى DTO وجلب بيانات البنك (المبلغ + تاريخ آخر دفع)
     */
    private ReportDTO processAndConvertToDTO(Report report) {
        String lastPaymentDateFromBank = null;

        // 1. جلب البيانات من Bank API
        try {
            String bankUrl = "http://localhost:8083/api/bank/status?cli=" + report.getCli();
            Map<String, Object> bankResponse = restTemplate.getForObject(bankUrl, Map.class);

            if (bankResponse != null) {
                // تحديث المبلغ المدفوع
                Double totalPaid = (bankResponse.get("totalPaid") != null) ?
                        Double.valueOf(bankResponse.get("totalPaid").toString()) : 0.0;
                report.setPaidAmount(totalPaid);

                // جلب تاريخ آخر عملية خلاص
                if (bankResponse.get("lastPaymentDate") != null) {
                    lastPaymentDateFromBank = bankResponse.get("lastPaymentDate").toString();
                }
            }
        } catch (Exception e) {
            System.err.println("Bank API Offline or CLI not found: " + report.getCli());
            report.setPaidAmount(0.0);
        }

        // 2. تحديث حالة التقرير بناءً على المعطيات الجديدة
        updatePaymentStatusBasedOnDate(report);

        // 3. بناء الـ DTO
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setCli(report.getCli());
        dto.setPoint(report.getPoint());

        double total = (report.getAmount() != null) ? report.getAmount() : 0.0;
        double paid = (report.getPaidAmount() != null) ? report.getPaidAmount() : 0.0;

        dto.setTotalAmount(total);
        dto.setPaidAmount(paid);
        dto.setRemainingAmount(total - paid);
        dto.setEngagementDate(report.getEngagementDate());
        dto.setLastPaymentDate(lastPaymentDateFromBank);

        if (report.getEngagementDate() != null && !report.getEngagementDate().isEmpty()) {
            dto.setDaysDiff(calculateDaysDifference(report.getEngagementDate()));
        } else {
            dto.setDaysDiff(0);
        }

        dto.setStatus(report.getStatus());
        dto.setObservation(report.getObservation());

        return dto;
    }

    /**
     * تحديث حالة الخلاص
     */
    private void updatePaymentStatusBasedOnDate(Report report) {
        double paid = (report.getPaidAmount() != null) ? report.getPaidAmount() : 0.0;
        double total = (report.getAmount() != null) ? report.getAmount() : 0.0;

        if (total > 0 && paid >= total) {
            report.setStatus(PaymentStatus.PAYE);
            return;
        }

        if (report.getEngagementDate() == null || report.getEngagementDate().isEmpty()) {
            report.setStatus(paid > 0 ? PaymentStatus.PARTIEL : PaymentStatus.NON_PAYE);
            return;
        }

        boolean isPast = isPastDate(report.getEngagementDate());
        if (isPast) {
            if ("Facilité de paiement".equals(report.getPoint())) {
                int nb = (report.getScheduleNumber() != null && report.getScheduleNumber() > 0) ? report.getScheduleNumber() : 1;
                if (paid >= (total / nb)) report.setStatus(PaymentStatus.PARTIEL);
                else report.setStatus(PaymentStatus.EN_RETARD);
            } else {
                report.setStatus(PaymentStatus.EN_RETARD);
            }
        } else {
            report.setStatus(paid > 0 ? PaymentStatus.PARTIEL : PaymentStatus.NON_PAYE);
        }
    }

    public long calculateDaysDifference(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate engDate = LocalDate.parse(dateStr, formatter);
            return ChronoUnit.DAYS.between(LocalDate.now(), engDate);
        } catch (Exception e) { return 0; }
    }

    public boolean isPastDate(String dateStr) {
        return calculateDaysDifference(dateStr) < 0;
    }

    // --- CRUD الأساسي ---

    public Report saveReport(Report report) {
        if (report.getCreationDate() == null) {
            report.setCreationDate(LocalDateTime.now());
        }
        report.setUpdatedDate(LocalDateTime.now());
        updatePaymentStatusBasedOnDate(report);
        return reportRepository.save(report);
    }

    // هذه الميثود هي التي كانت ناقصة وتسببت في خطأ الـ Controller
    public Report getReportById(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report introuvable avec l'ID: " + id));
    }

    public void deleteReport(UUID id) {
        reportRepository.deleteById(id);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }
}