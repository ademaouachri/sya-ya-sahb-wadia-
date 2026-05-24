package com.example.backend.Repository;

import com.example.backend.Model.Client;
import com.example.backend.DTO.DashboardStats;
import com.example.backend.DTO.MonthlyEvolutionDTO;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    Optional<Client> findByCin(String cin);

    // هذه الميثود بقت كما هي لاستخدامها في الـ Dashboard التقليدي
    @Query("""
        SELECT new com.example.backend.DTO.DashboardStats(
            COUNT(DISTINCT c.cli),
            SUM(c.totalImpayeAmount),
            SUM(c.totalSdbAmount),
            SUM(c.engagementGlobal),
            SUM(c.montantAutorise),
            SUM(c.encours),
            SUM(c.montantDepassement)
        )
        FROM Client c
        LEFT JOIN Report r ON c.cli = r.cli
        WHERE (:agencyCodes IS NULL OR c.agencyCode IN :agencyCodes)
        AND (:zoneCodes IS NULL OR c.zoneCode IN :zoneCodes)
        AND (:regionCodes IS NULL OR c.regionCode IN :regionCodes)
        AND (:activityCodes IS NULL OR c.activityCode IN :activityCodes)
        AND (:marcheCodes IS NULL OR c.marchetCode IN :marcheCodes)
        AND (:segmentCodes IS NULL OR c.segmentCode IN :segmentCodes)
        AND (:businessCenterCodes IS NULL OR c.businessCenterCode IN :businessCenterCodes)
        AND (:postalCode IS NULL OR c.postalCode = :postalCode)
        AND (:isCloture IS NULL OR c.isCloture = :isCloture)
        AND (:structure IS NULL OR TRIM(UPPER(c.structure)) = TRIM(UPPER(:structure)))
        AND (:fullName IS NULL OR UPPER(c.fullName) LIKE UPPER(CONCAT('%', :fullName, '%')))
        AND (:clientGroup IS NULL OR c.clientGroup = :clientGroup)
        AND (:createdBy IS NULL OR c.createdBy = :createdBy)
        AND (cast(:startDate as timestamp) IS NULL OR c.createdAt >= :startDate)
    """)
    DashboardStats getDashboardGlobalStats(
            @Param("agencyCodes") List<String> agencyCodes,
            @Param("zoneCodes") List<String> zoneCodes,
            @Param("regionCodes") List<String> regionCodes,
            @Param("activityCodes") List<String> activityCodes,
            @Param("marcheCodes") List<String> marcheCodes,
            @Param("segmentCodes") List<String> segmentCodes,
            @Param("businessCenterCodes") List<String> businessCenterCodes,
            @Param("postalCode") String postalCode,
            @Param("isCloture") String isCloture,
            @Param("structure") String structure,
            @Param("fullName") String fullName,
            @Param("clientGroup") String clientGroup,
            @Param("createdBy") String createdBy,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
        SELECT new com.example.backend.DTO.MonthlyEvolutionDTO(
            YEAR(c.createdAt),
            MONTH(c.createdAt), 
            SUM(c.totalImpayeAmount), 
            SUM(c.totalSdbAmount), 
            SUM(c.engagementGlobal),
            SUM(CASE WHEN c.isCloture = 'Y' THEN c.engagementGlobal ELSE 0.0 END)
        )
        FROM Client c
        WHERE (:agencyCodes IS NULL OR c.agencyCode IN :agencyCodes)
        AND (:zoneCodes IS NULL OR c.zoneCode IN :zoneCodes)
        AND (:regionCodes IS NULL OR c.regionCode IN :regionCodes)
        AND (:structure IS NULL OR TRIM(UPPER(c.structure)) = TRIM(UPPER(:structure)))
        GROUP BY YEAR(c.createdAt), MONTH(c.createdAt)
        ORDER BY YEAR(c.createdAt) ASC, MONTH(c.createdAt) ASC
    """)
    List<MonthlyEvolutionDTO> getDashboardMonthlyStats(
            @Param("agencyCodes") List<String> agencyCodes,
            @Param("zoneCodes") List<String> zoneCodes,
            @Param("regionCodes") List<String> regionCodes,
            @Param("structure") String structure
    );

    @Query("""
        SELECT c FROM Client c
        WHERE (:agencyCodes IS NULL OR c.agencyCode IN :agencyCodes)
        AND (:zoneCodes IS NULL OR c.zoneCode IN :zoneCodes)
        AND (:regionCodes IS NULL OR c.regionCode IN :regionCodes)
        AND (:activityCodes IS NULL OR c.activityCode IN :activityCodes)
        AND (:marcheCodes IS NULL OR c.marchetCode IN :marcheCodes)
        AND (:segmentCodes IS NULL OR c.segmentCode IN :segmentCodes)
        AND (:businessCenterCodes IS NULL OR c.businessCenterCode IN :businessCenterCodes)
        AND (:postalCode IS NULL OR c.postalCode = :postalCode)
        AND (:isCloture IS NULL OR c.isCloture = :isCloture)
        AND (:structure IS NULL OR TRIM(UPPER(c.structure)) = TRIM(UPPER(:structure)))
        AND (:fullName IS NULL OR UPPER(c.fullName) LIKE UPPER(CONCAT('%', :fullName, '%')))
        AND (:clientGroup IS NULL OR c.clientGroup = :clientGroup)
        AND (:createdBy IS NULL OR c.createdBy = :createdBy)
        AND (cast(:startDate as timestamp) IS NULL OR c.createdAt >= :startDate)
    """)
    List<Client> findByPerimetre(
            @Param("agencyCodes") List<String> agencyCodes,
            @Param("zoneCodes") List<String> zoneCodes,
            @Param("regionCodes") List<String> regionCodes,
            @Param("activityCodes") List<String> activityCodes,
            @Param("marcheCodes") List<String> marcheCodes,
            @Param("segmentCodes") List<String> segmentCodes,
            @Param("businessCenterCodes") List<String> businessCenterCodes,
            @Param("postalCode") String postalCode,
            @Param("isCloture") String isCloture,
            @Param("structure") String structure,
            @Param("fullName") String fullName,
            @Param("clientGroup") String clientGroup,
            @Param("createdBy") String createdBy,
            @Param("startDate") LocalDateTime startDate,
            Sort sort
    );

    @Modifying
    @Query(value = "UPDATE client SET total_sdb_amount = 0, total_days_sdb = 0 WHERE cli = :cli", nativeQuery = true)
    void zeroOutSdbForClient(@Param("cli") String cli);

    @Query("""
        SELECT 
            COUNT(c), 
            COALESCE(SUM(c.totalImpayeAmount), 0.0), 
            COALESCE(SUM(c.totalSdbAmount), 0.0), 
            COALESCE(SUM(c.engagementGlobal), 0.0)
        FROM Client c
        WHERE (:matricule IS NULL OR c.createdBy = :matricule)
    """)
    Object getRecouvreurFinancials(@Param("matricule") String matricule);
}