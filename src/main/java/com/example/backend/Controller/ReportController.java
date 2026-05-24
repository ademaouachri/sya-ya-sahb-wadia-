package com.example.backend.Controller;

import com.example.backend.DTO.ReportDTO;
import com.example.backend.Model.Report;
import com.example.backend.Service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*") // استعمل "*" في مرحلة الـ Dev باش تتفادى مشاكل الـ CORS مع Angular
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 🔥 جلب كل التقارير بصيغة DTO مع إمكانية الفلترة بالـ Point والـ Status
     * الـ Service هنا هو اللي يكلم الـ Bank API ويحدث المبالغ والحالة تلقائياً
     * * أمثلة للطلب م الـ Front أو Postman:
     * - /api/reports/all (كل شيء)
     * - /api/reports/all?point=P002 (فلتر حسب الإجراء)
     * - /api/reports/all?status=EN_RETARD (فلتر حسب الحالة)
     * - /api/reports/all?point=P003&status=PARTIEL (الفلاتر الزوز مع بعضهم)
     */
    @GetMapping("/all")
    public ResponseEntity<List<ReportDTO>> getAllReports(
            @RequestParam(required = false) String point,
            @RequestParam(required = false) String status) {

        // نعيطوا للميثود المفلترة والذكية في الـ Service
        List<ReportDTO> reports = reportService.getAllReportsWithAutoCheck(point, status);
        return ResponseEntity.ok(reports);
    }

    /**
     * إضافة تقرير جديد (PFE Report)
     */
    @PostMapping("/add")
    public ResponseEntity<Report> createReport(@RequestBody Report report) {
        // الـ Service باش يتكفل بحساب الحالة الابتدائية وتخزين التقرير والـ Client معاً
        Report savedReport = reportService.saveReport(report);
        return ResponseEntity.ok(savedReport);
    }

    /**
     * جلب تقرير واحد بالـ ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable UUID id) {
        Report report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    /**
     * حذف تقرير
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ميثود مساعدة للـ Frontend لحساب فارق الأيام
     */
    @GetMapping("/check-date")
    public ResponseEntity<Long> getDaysDifference(@RequestParam String date) {
        long diff = reportService.calculateDaysDifference(date);
        return ResponseEntity.ok(diff);
    }

    /**
     * محاكاة لعملية دفع (للتجربة فقط)
     * الدفع الحقيقي لازم يصير في الـ Bank API (Port 8083)
     */
    @PostMapping("/pay-mock")
    public ResponseEntity<String> mockPayment() {
        return ResponseEntity.ok("Payment process should be handled by the Bank API for CLI consistency.");
    }
}