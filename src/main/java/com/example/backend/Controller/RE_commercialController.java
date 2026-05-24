package com.example.backend.Controller;

import com.example.backend.DTO.DashboardStats;
import com.example.backend.Model.Client;
import com.example.backend.Model.Utilisateur;
import com.example.backend.Service.RE_commercialService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/re-commercial")
@CrossOrigin(origins = "*")
public class RE_commercialController {

    private final RE_commercialService reCommercialService;

    public RE_commercialController(RE_commercialService reCommercialService) {
        this.reCommercialService = reCommercialService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboardStats(
            @AuthenticationPrincipal Utilisateur utilisateur,
            @RequestParam(required = false) String structure,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate
    ) {
        return ResponseEntity.ok(reCommercialService.getDashboardData(utilisateur, structure, startDate));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getClients(
            @AuthenticationPrincipal Utilisateur utilisateur,
            @RequestParam(required = false) String agencyCode,
            @RequestParam(required = false) String activityCode,
            @RequestParam(required = false) String marcheCode,
            @RequestParam(required = false) String segmentCode,
            @RequestParam(required = false) String businessCenterCode,
            @RequestParam(required = false) String zoneCode,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String isCloture,
            @RequestParam(required = false) String structure,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String clientGroup,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(defaultValue = "totalImpayeAmount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        List<Client> clients = reCommercialService.getClientsForUser(
                utilisateur, agencyCode, activityCode, marcheCode, segmentCode,
                businessCenterCode, zoneCode, regionCode, postalCode, isCloture,
                structure, fullName, clientGroup, createdBy, startDate, sortBy, sortDir
        );
        return ResponseEntity.ok(clients);
    }
}