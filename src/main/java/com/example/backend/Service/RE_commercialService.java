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

        String effectiveStructure = extractStructure(utilisateur);
        List<String> agencyCodes = extractAgencyCodes(utilisateur);
        List<String> zoneCodes = extractZoneCodes(utilisateur);
        List<String> regionCodes = extractRegionCodes(utilisateur);

        List<String> activityCodes = (profil.getActivite() == 1 && utilisateur.getActivites() != null) ?
                utilisateur.getActivites().stream().map(Activite::getCode).toList() : null;
        List<String> marcheCodes = (profil.getMarche() == 1 && utilisateur.getMarches() != null) ?
                utilisateur.getMarches().stream().map(Marche::getCode).toList() : null;
        List<String> segmentCodes = (profil.getSegment() == 1 && utilisateur.getSegments() != null) ?
                utilisateur.getSegments().stream().map(Segment::getCode).toList() : null;
        List<String> businessCenterCodes = (profil.getCentreAffaire() == 1 && utilisateur.getCentreAffaires() != null) ?
                utilisateur.getCentreAffaires().stream().map(CentreAffaire::getCode).toList() : null;

        // استدعاء الـ Repository بالتعديلات الجديدة
        DashboardStats stats = clientRepository.getDashboardGlobalStats(
                agencyCodes, zoneCodes, regionCodes, activityCodes, marcheCodes,
                segmentCodes, businessCenterCodes, null, "N", effectiveStructure, null, null, null, startDate
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
    public List<Client> getClientsForUser(Utilisateur utilisateur, String agencyCode, String activityCode,
                                          String marcheCode, String segmentCode, String businessCenterCode,
                                          String zoneCode, String regionCode, String postalCode, String isCloture,
                                          String structure, String fullName, String clientGroup, String createdBy,
                                          LocalDateTime startDate, String sortBy, String sortDir) {

        Profil profil = utilisateur.getProfil();
        if (profil == null) return List.of();

        String effectiveStructure = extractStructure(utilisateur);
        List<String> agencyCodes = extractAgencyCodes(utilisateur);
        List<String> zoneCodes = extractZoneCodes(utilisateur);
        List<String> regionCodes = extractRegionCodes(utilisateur);

        if (zoneCode != null && !zoneCode.isBlank()) zoneCodes = List.of(zoneCode);
        if (regionCode != null && !regionCode.isBlank()) regionCodes = List.of(regionCode);
        if (agencyCode != null && !agencyCode.isBlank()) agencyCodes = List.of(agencyCode);

        List<String> activityCodes = (profil.getActivite() == 1 && utilisateur.getActivites() != null) ?
                utilisateur.getActivites().stream().map(Activite::getCode).toList() : null;
        List<String> marcheCodes = (profil.getMarche() == 1 && utilisateur.getMarches() != null) ?
                utilisateur.getMarches().stream().map(Marche::getCode).toList() : null;
        List<String> segmentCodes = (profil.getSegment() == 1 && utilisateur.getSegments() != null) ?
                utilisateur.getSegments().stream().map(Segment::getCode).toList() : null;
        List<String> businessCenterCodes = (profil.getCentreAffaire() == 1 && utilisateur.getCentreAffaires() != null) ?
                utilisateur.getCentreAffaires().stream().map(CentreAffaire::getCode).toList() : null;

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) sortBy = "totalImpayeAmount";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        return clientRepository.findByPerimetre(
                agencyCodes, zoneCodes, regionCodes, activityCodes, marcheCodes,
                segmentCodes, businessCenterCodes, postalCode, isCloture,
                effectiveStructure, fullName, clientGroup, createdBy, startDate, sort
        );
    }

    private String extractStructure(Utilisateur u) {
        return (u.getProfil().getStructure() != null) ? u.getProfil().getStructure().name() : null;
    }

    private List<String> extractAgencyCodes(Utilisateur u) {
        return (u.getProfil().getAgence() == 1 && u.getAgences() != null) ? u.getAgences().stream().map(Agence::getCode).toList() : null;
    }

    private List<String> extractZoneCodes(Utilisateur u) {
        return (u.getProfil().getZone() == 1 && u.getZones() != null) ? u.getZones().stream().map(Zone::getCode).toList() : null;
    }

    private List<String> extractRegionCodes(Utilisateur u) {
        return (u.getProfil().getRegion() == 1 && u.getRegions() != null) ? u.getRegions().stream().map(Region::getCode).toList() : null;
    }
}