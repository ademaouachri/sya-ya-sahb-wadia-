package com.example.backend.Service;

import com.example.backend.DTO.ReportDTO;
import com.example.backend.Model.PaymentStatus;
import com.example.backend.Model.Report;
import com.example.backend.Repository.ClientRepository;
import com.example.backend.Repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final ClientRepository clientRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public List<ReportDTO> getAllReportsWithAutoCheck(String point, String statusStr) {
        PaymentStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = PaymentStatus.valueOf(statusStr.trim());
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Status inconnu: {}", statusStr);
            }
        }
        String filterPoint = (point != null && !point.trim().isEmpty()) ? point.trim() : null;
        return reportRepository.findAllFiltered(filterPoint, status).stream()
                .map(report -> processAndConvertToDTO(report, true))
                .collect(Collectors.toList());
    }

    public ReportDTO processAndConvertToDTO(Report report, boolean checkBank) {
        String lastPaymentDateFromBank = null;
        if (checkBank && report.getStatus() != PaymentStatus.PAYE) {
            try {
                if (report.getCli() != null) {
                    String cleanCli = report.getCli().trim().replace("'", "");
                    String bankUrl = "http://localhost:8083/api/bank/status?cli=" + cleanCli;
                    Map<String, Object> bankResponse = restTemplate.getForObject(bankUrl, Map.class);
                    if (bankResponse != null) {
                        Double totalPaid = 0.0;
                        if (bankResponse.get("totalPaid") != null) totalPaid = Double.valueOf(bankResponse.get("totalPaid").toString());
                        else if (bankResponse.get("amount") != null) totalPaid = Double.valueOf(bankResponse.get("amount").toString());

                        report.setPaidAmount(totalPaid);
                        updatePaymentStatus(report);
                        reportRepository.save(report);
                    }
                }
            } catch (Exception e) { log.error("Bank Offline for CLI: {}", report.getCli()); }
        }

        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setCli(report.getCli());
        dto.setPoint(report.getPoint());
        double total = (report.getAmount() != null) ? report.getAmount() : 0.0;
        double paid = (report.getPaidAmount() != null) ? report.getPaidAmount() : 0.0;
        dto.setTotalAmount(total);
        dto.setPaidAmount(paid);
        dto.setRemainingAmount(Math.max(0, total - paid));
        dto.setEngagementDate(report.getEngagementDate());
        dto.setLastPaymentDate(lastPaymentDateFromBank);
        dto.setObservation(report.getObservation());

        dto.setDaysDiff(report.getStatus() == PaymentStatus.PAYE ? 0L : calculateDaysDifference(report.getEngagementDate()));
        dto.setStatus(report.getStatus());
        dto.setVisitDate(report.getVisitDate());
        dto.setVisitTime(report.getVisitTime());
        dto.setLocation(report.getLocation());
        return dto;
    }

    @Transactional
    public Report saveReport(Report report) {
        if (report.getCreationDate() == null) report.setCreationDate(LocalDateTime.now());
        report.setUpdatedDate(LocalDateTime.now());
        updatePaymentStatus(report);
        return reportRepository.save(report);
    }

    @Transactional
    public void updatePaymentStatus(Report report) {
        // 1. حساب الإجمالي للعميل من كل تقاريره لضمان دقة البيانات
        List<Report> allReportsForClient = reportRepository.findByCli(report.getCli());
        double totalAll = allReportsForClient.stream()
                .mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0)
                .sum();
        double paidAll = allReportsForClient.stream()
                .mapToDouble(r -> r.getPaidAmount() != null ? r.getPaidAmount() : 0.0)
                .sum();

        // 2. حساب الـ montantDepassement للتقرير الحالي (الأقساط الفائتة)
        if ("P003".equals(report.getPoint()) && report.getEcheances() != null) {
            LocalDate today = LocalDate.now();
            double totalDueNow = report.getEcheances().stream()
                    .filter(e -> LocalDate.parse(e.getDateEcheance()).isBefore(today))
                    .mapToDouble(e -> e.getMontantEcheance())
                    .sum();

            double currentPaid = (report.getPaidAmount() != null) ? report.getPaidAmount() : 0.0;
            report.setMontantDepassement(Math.max(0, totalDueNow - currentPaid));
        } else {
            report.setMontantDepassement(0.0);
        }

        // 3. تحديث بيانات العميل في الـ Database
        clientRepository.findById(report.getCli()).ifPresent(client -> {
            // تحديث الـ Encours بالمبلغ المتبقي الكلي
            double remainingAll = Math.max(0, totalAll - paidAll);
            client.setEncours(BigDecimal.valueOf(remainingAll));

            // تحديث الـ montantDepassement في العميل (يأخذ قيمة التقرير الحالي)
            client.setMontantDepassement(BigDecimal.valueOf(report.getMontantDepassement()));

            // تحديث الـ Cloture (يغلق العميل فقط إذا سدد مجموع كل تقاريره)
            client.setIsCloture((totalAll > 0 && paidAll >= totalAll) ? "Y" : "N");

            // الـ Flags (تبقى حسب الـ Point)
            if ("P004".equals(report.getPoint())) {
                client.setTraite("Y");
                client.setContactFlag("N");
            } else if (List.of("P001", "P002", "P003").contains(report.getPoint())) {
                client.setTraite("Y");
                client.setContactFlag("Y");
            }
            clientRepository.save(client);
        });

        // 4. تحديد حالة التقرير الحالي (Status)
        double currentPaid = (report.getPaidAmount() != null) ? report.getPaidAmount() : 0.0;
        double currentTotal = (report.getAmount() != null) ? report.getAmount() : 0.0;

        if (currentTotal > 0 && currentPaid >= currentTotal) {
            report.setStatus(PaymentStatus.PAYE);
        } else if (report.getMontantDepassement() > 0) {
            report.setStatus(PaymentStatus.EN_RETARD);
        } else {
            if (report.getEngagementDate() == null || report.getEngagementDate().isEmpty()) {
                report.setStatus(currentPaid > 0 ? PaymentStatus.PARTIEL : PaymentStatus.NON_PAYE);
            } else {
                long diff = calculateDaysDifference(report.getEngagementDate());
                if (diff < 0) {
                    if ("P003".equals(report.getPoint())) {
                        int nb = (report.getScheduleNumber() != null && report.getScheduleNumber() > 0) ? report.getScheduleNumber() : 1;
                        report.setStatus((currentPaid >= (currentTotal / nb) && currentPaid < currentTotal) ? PaymentStatus.PARTIEL : PaymentStatus.EN_RETARD);
                    } else {
                        report.setStatus(currentPaid > 0 ? PaymentStatus.PARTIEL : PaymentStatus.EN_RETARD);
                    }
                } else {
                    report.setStatus(currentPaid > 0 ? PaymentStatus.PARTIEL : PaymentStatus.NON_PAYE);
                }
            }
        }
    }
    public long calculateDaysDifference(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) return 0;
            return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dateStr.split(" ")[0]));
        } catch (Exception e) { return 0; }
    }

    public Report getReportById(UUID id) {
        return reportRepository.findById(id).orElseThrow(() -> new RuntimeException("Report introuvable"));
    }

    public void deleteReport(UUID id) { reportRepository.deleteById(id); }

    public List<Report> getAllReports() { return reportRepository.findAll(); }
}