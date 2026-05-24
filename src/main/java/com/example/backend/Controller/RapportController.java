package com.example.backend.Controller;

import com.example.backend.Model.Utilisateur;
import com.example.backend.Service.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping("/download-weekly")
    public ResponseEntity<byte[]> downloadWeeklyReport(@AuthenticationPrincipal Utilisateur utilisateur) {

        // توليد الـ PDF
        byte[] pdf = rapportService.generateWeeklyPdf(utilisateur);

        // إعداد اسم الملف باستعمال Matricule و Nom (تجنبنا getUsername غير الموجودة)
        String fileName = "Rapport_Hebdo_" + utilisateur.getNom() + "_" + utilisateur.getMatricule() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}///C:\Rapports_GTI