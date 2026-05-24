package com.example.backend.Repository;

import com.example.backend.Model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByCli(String cli);


    List<Report> findByCreatedByAndCreationDateBetween(String createdBy, LocalDateTime start, LocalDateTime end);

    // 1. الحسبة الإجمالية مصلحة: تقبل الـ matricule والـ point معاً 🔥
    @Query("""
        SELECT 
            COUNT(DISTINCT r.cli), 
            COALESCE(SUM(r.amount), 0.0), 
            COALESCE(SUM(r.paidAmount), 0.0)
        FROM Report r 
        WHERE r.createdBy = :matricule
          AND (:point IS NULL OR r.point = :point)
    """)
    Object getGlobalReportStats(@Param("matricule") String matricule, @Param("point") String point);

    // 2. عدد الحرفاء الأسبوعي مصلحة: تقبل الـ point 🔥
    @Query("""
        SELECT COUNT(DISTINCT r.cli) 
        FROM Report r 
        WHERE r.createdBy = :matricule 
          AND r.creationDate BETWEEN :startOfWeek AND :endOfWeek
          AND (:point IS NULL OR r.point = :point)
    """)
    Long countWeeklyClients(@Param("matricule") String matricule,
                            @Param("startOfWeek") LocalDateTime startOfWeek,
                            @Param("endOfWeek") LocalDateTime endOfWeek,
                            @Param("point") String point);

    // 3. إجمالي المقبوضات الأسبوعية مصلحة: تقبل الـ point 🔥
    @Query("""
        SELECT COALESCE(SUM(r.paidAmount), 0.0) 
        FROM Report r 
        WHERE r.createdBy = :matricule 
          AND r.creationDate BETWEEN :startOfWeek AND :endOfWeek
          AND (:point IS NULL OR r.point = :point)
    """)
    Double sumWeeklyPaidAmount(@Param("matricule") String matricule,
                               @Param("startOfWeek") LocalDateTime startOfWeek,
                               @Param("endOfWeek") LocalDateTime endOfWeek,
                               @Param("point") String point);

    // 4. إجمالي عدد التقارير مصلحة: تقبل الـ point 🔥
    @Query("""
        SELECT COUNT(r) 
        FROM Report r 
        WHERE r.createdBy = :matricule 
          AND (:point IS NULL OR r.point = :point)
    """)
    Long countTotalReportsGlobal(@Param("matricule") String matricule, @Param("point") String point);

    // 5. عدد التقارير الأسبوعية مصلحة: تقبل الـ point 🔥
    @Query("""
        SELECT COUNT(r) 
        FROM Report r 
        WHERE r.createdBy = :matricule 
          AND r.creationDate BETWEEN :startOfWeek AND :endOfWeek
          AND (:point IS NULL OR r.point = :point)
    """)
    Long countTotalReportsWeekly(@Param("matricule") String matricule,
                                 @Param("startOfWeek") LocalDateTime startOfWeek,
                                 @Param("endOfWeek") LocalDateTime endOfWeek,
                                 @Param("point") String point);

    // 6. توزيع الحالات (Donut Chart) مصلحة: تقبل الـ point 🔥
    @Query("""
        SELECT r.status, COUNT(r) 
        FROM Report r 
        WHERE r.createdBy = :matricule 
          AND (:point IS NULL OR r.point = :point)
        GROUP BY r.status
    """)
    List<Object[]> countStatusForRecouvreur(@Param("matricule") String matricule, @Param("point") String point);

    // 7. الفلترة الجديدة للـ Reports List (اللي زدناها توا) 🔥
    @Query("""
        SELECT r FROM Report r 
        WHERE (:point IS NULL OR r.point = :point)
          AND (:status IS NULL OR r.status = :status)
        ORDER BY r.creationDate DESC
    """)
    List<Report> findAllFiltered(@Param("point") String point, @Param("status") com.example.backend.Model.PaymentStatus status);

    // 8. توزيع الإجراءات يقعد ديما يورّي كل شي باش الـ Agent يقارن خِدمته
    @Query("SELECT r.point, COUNT(r) FROM Report r WHERE r.createdBy = :matricule GROUP BY r.point")
    List<Object[]> countPointsForRecouvreur(@Param("matricule") String matricule);

    // 9. العدادات النشطة
    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdBy = :matricule AND r.point = 'P002' AND r.status != 'PAYE'")
    Long countActivePromessesForRecouvreur(@Param("matricule") String matricule);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdBy = :matricule AND r.point = 'P003' AND r.status != 'PAYE'")
    Long countActiveFacilitesForRecouvreur(@Param("matricule") String matricule);
    List<Report> findByCreationDateBetween(LocalDateTime start, LocalDateTime end);

}