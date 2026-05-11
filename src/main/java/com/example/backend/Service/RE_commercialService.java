package com.example.backend.Service;

import com.example.backend.DTO.DashboardStats;
import com.example.backend.DTO.MonthlyEvolutionDTO;
import com.example.backend.Model.*;
import com.example.backend.Repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RE_commercialService {

    private final ClientRepository clientRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "totalImpayeAmount", "totalCommitment", "totalAuthorization",
            "outstanding", "totalDaysImpaye", "totalDaysSdb",
            "createdAt", "updatedAt", "totalDepassement", "totalSdbAmount"
    );

    @Transactional(readOnly = true)
    public DashboardStats getDashboardData(Utilisateur utilisateur, String structure, LocalDateTime startDate) {
        Profil profil = utilisateur.getProfil();
        if (profil == null) return new DashboardStats();

        // استخراج الصلاحيات (نفس المنطق المستعمل في getClientsForUser)
        String effectiveStructure = extractStructure(utilisateur);
        List<String> agencyCodes = extractAgencyCodes(utilisateur);
        List<String> zoneCodes = extractZoneCodes(utilisateur);
        List<String> regionCodes = extractRegionCodes(utilisateur);

        // إضافة فلاتر النشاط والسوق والقطاع لضمان أن الإحصائيات تشمل الـ 4 عملاء فقط
        List<String> activityCodes = (profil.getActivite() == 1 && utilisateur.getActivites() != null) ?
                utilisateur.getActivites().stream().map(Activite::getCode).toList() : null;
        List<String> marcheCodes = (profil.getMarche() == 1 && utilisateur.getMarches() != null) ?
                utilisateur.getMarches().stream().map(Marche::getCode).toList() : null;
        List<String> segmentCodes = (profil.getSegment() == 1 && utilisateur.getSegments() != null) ?
                utilisateur.getSegments().stream().map(Segment::getCode).toList() : null;
        List<String> businessCenterCodes = (profil.getCentreAffaire() == 1 && utilisateur.getCentreAffaires() != null) ?
                utilisateur.getCentreAffaires().stream().map(CentreAffaire::getCode).toList() : null;

        // تمرير كافة المعايير لـ getDashboardGlobalStats لضمان دقة الأرقام
        DashboardStats stats = clientRepository.getDashboardGlobalStats(
                agencyCodes, zoneCodes, regionCodes,
                activityCodes, marcheCodes, segmentCodes, businessCenterCodes,
                null, null, effectiveStructure, null, null, null, startDate
        );

        List<MonthlyEvolutionDTO> monthlyStats = clientRepository.getDashboardMonthlyStats(
                agencyCodes, zoneCodes, regionCodes, effectiveStructure
        );

        if (stats != null) {
            stats.setMonthlyEvolution(monthlyStats);
        } else {
            stats = new DashboardStats();
            stats.setMonthlyEvolution(monthlyStats);
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Client> getClientsForUser(Utilisateur utilisateur,
                                          String agencyCode,
                                          String activityCode,
                                          String marcheCode,
                                          String segmentCode,
                                          String businessCenterCode,
                                          String zoneCode,
                                          String regionCode,
                                          String postalCode,
                                          String dossierType,
                                          String structure,
                                          String fullName,
                                          String clientGroup,
                                          String createdBy,
                                          LocalDateTime startDate,
                                          String sortBy,
                                          String sortDir) {

        Profil profil = utilisateur.getProfil();
        if (profil == null) return List.of();

        String effectiveStructure = extractStructure(utilisateur);

        List<String> agencyCodes = extractAgencyCodes(utilisateur);
        List<String> zoneCodes = extractZoneCodes(utilisateur);
        List<String> regionCodes = extractRegionCodes(utilisateur);

        if (zoneCode != null && !zoneCode.trim().isEmpty()) {
            if (zoneCodes != null && !zoneCodes.contains(zoneCode)) {
                return List.of();
            }
            zoneCodes = List.of(zoneCode);
        }

        if (regionCode != null && !regionCode.trim().isEmpty()) {
            if (regionCodes != null && !regionCodes.contains(regionCode)) {
                return List.of();
            }
            regionCodes = List.of(regionCode);
        }

        List<String> activityCodes = (profil.getActivite() == 1 && utilisateur.getActivites() != null) ?
                utilisateur.getActivites().stream().map(Activite::getCode).toList() : null;
        List<String> marcheCodes = (profil.getMarche() == 1 && utilisateur.getMarches() != null) ?
                utilisateur.getMarches().stream().map(Marche::getCode).toList() : null;
        List<String> segmentCodes = (profil.getSegment() == 1 && utilisateur.getSegments() != null) ?
                utilisateur.getSegments().stream().map(Segment::getCode).toList() : null;
        List<String> businessCenterCodes = (profil.getCentreAffaire() == 1 && utilisateur.getCentreAffaires() != null) ?
                utilisateur.getCentreAffaires().stream().map(CentreAffaire::getCode).toList() : null;

        if (agencyCode != null && !agencyCode.trim().isEmpty()) {
            if (agencyCodes != null && !agencyCodes.contains(agencyCode)) {
                return List.of();
            }
            agencyCodes = List.of(agencyCode);
        }

        if (activityCode != null && !activityCode.trim().isEmpty()) {
            if (activityCodes != null && !activityCodes.contains(activityCode)) {
                return List.of();
            }
            activityCodes = List.of(activityCode);
        }

        if (marcheCode != null && !marcheCode.trim().isEmpty()) {
            if (marcheCodes != null && !marcheCodes.contains(marcheCode)) {
                return List.of();
            }
            marcheCodes = List.of(marcheCode);
        }

        if (segmentCode != null && !segmentCode.trim().isEmpty()) {
            if (segmentCodes != null && !segmentCodes.contains(segmentCode)) {
                return List.of();
            }
            segmentCodes = List.of(segmentCode);
        }

        if (businessCenterCode != null && !businessCenterCode.trim().isEmpty()) {
            if (businessCenterCodes != null && !businessCenterCodes.contains(businessCenterCode)) {
                return List.of();
            }
            businessCenterCodes = List.of(businessCenterCode);
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "totalImpayeAmount";
        }

        Sort sort = "asc".equalsIgnoreCase(sortDir) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        return clientRepository.findByPerimetre(
                agencyCodes, zoneCodes, regionCodes, activityCodes, marcheCodes,
                segmentCodes, businessCenterCodes, postalCode, dossierType,
                effectiveStructure, fullName, clientGroup, createdBy, startDate,
                sort
        );
    }

    // ==================== Extract Methods ====================

    private String extractStructure(Utilisateur u) {
        if (u.getProfil().getStructure() != null) {
            return u.getProfil().getStructure().name();
        }
        return null;
    }

    private List<String> extractAgencyCodes(Utilisateur u) {
        if (u.getProfil().getAgence() == 1 && u.getAgences() != null) {
            return u.getAgences().stream().map(Agence::getCode).toList();
        }
        return null;
    }

    private List<String> extractZoneCodes(Utilisateur u) {
        if (u.getProfil().getZone() == 1 && u.getZones() != null) {
            return u.getZones().stream().map(Zone::getCode).toList();
        }
        return null;
    }

    private List<String> extractRegionCodes(Utilisateur u) {
        if (u.getProfil().getRegion() == 1 && u.getRegions() != null) {
            return u.getRegions().stream().map(Region::getCode).toList();
        }
        return null;
    }
}