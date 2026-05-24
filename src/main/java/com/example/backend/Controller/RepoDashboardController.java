package com.example.backend.Controller;

import com.example.backend.DTO.RepoDashboardDTO;
import com.example.backend.Model.Utilisateur;
import com.example.backend.Service.RepoDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repo-dashboard")
@CrossOrigin(origins = "*") // مريغل لمنع مشاكل الـ CORS مع الـ Angular في الـ Front
public class RepoDashboardController {

    private final RepoDashboardService repoDashboardService;

    // Injection متاع الـ Service
    public RepoDashboardController(RepoDashboardService repoDashboardService) {
        this.repoDashboardService = repoDashboardService;
    }

    /**
     * جلب إحصائيات الـ Recouvreur الحالي المتصل بالسيستم مع إمكانية الفلترة بالـ Point
     * الـ Endpoint هذا باش يقرى الـ Token ويجيب الـ Agent أوتوماتيكياً
     * * 🔥 أمثلة للـ URLs:
     * - بدون فلتر (كل شي): GET http://localhost:8081/api/repo-dashboard/stats
     * - بالفلتر (P002 مثلاً): GET http://localhost:8081/api/repo-dashboard/stats?point=P002
     */
    @GetMapping("/stats")
    public ResponseEntity<RepoDashboardDTO> getDashboardStats(
            @AuthenticationPrincipal Utilisateur utilisateur,
            @RequestParam(required = false) String point) { // 🔥 زدنا الـ Paramètre الجديد والـ optional هوني

        // نعديو الـ utilisateur والـ point مع بعضهم للـ Service المصلح
        RepoDashboardDTO stats = repoDashboardService.getRecouvreurStats(utilisateur, point);
        return ResponseEntity.ok(stats);
    }
}