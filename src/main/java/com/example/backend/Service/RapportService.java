package com.example.backend.Service;

import com.example.backend.DTO.RepoDashboardDTO;
import com.example.backend.DTO.ReportDTO;
import com.example.backend.Model.*;
import com.example.backend.Repository.ReportRepository;
import com.example.backend.Repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RapportService {

    private final ReportRepository reportRepository;
    private final ReportService reportService;
    private final RepoDashboardService repoDashboardService;
    private final TemplateEngine templateEngine;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    /**
     * Tâche planifiée pour générer et envoyer automatiquement les rapports hebdomadaires.
     * Fréquence actuelle : Toutes les 3 minutes (180000 ms) pour les tests.
     */
    @Scheduled(fixedRate = 180000)
    @Transactional(readOnly = true)
    public void autoGenerateAndSendReports() {

        // 1. Récupération de la liste des matricules uniques ayant créé des rapports (les agents actifs)
        List<String> distinctMatricules = reportRepository.findAll().stream()
                .map(Report::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        // 2. Recherche dynamique du manager/admin via la structure globale "S001"
        String managerEmail = null;
        String managerMatricule = "";

        List<Utilisateur> tousLesUtilisateurs = utilisateurRepository.findAll();
        for (Utilisateur u : tousLesUtilisateurs) {
            if (u.getProfil() != null && u.getProfil().getStructure() != null
                    && "S001".equalsIgnoreCase(u.getProfil().getStructure().name())) {
                managerEmail = u.getEmail();
                managerMatricule = u.getMatricule();
                break; // Manager S001 trouvé avec succès
            }
        }

        // Journalisation de l'état du Manager S001
        if (managerEmail != null) {
            System.out.println("📬 [Manager Status] Email du manager S001 trouvé : " + managerEmail + " (Matricule: " + managerMatricule + ")");
        } else {
            System.err.println("⚠️ [Manager Status] Aucun utilisateur trouvé avec la structure 'S001' en base de données !");
        }

        // 3. Boucle sur chaque matricule d'agent trouvé pour traiter et envoyer les rapports
        for (String matricule : distinctMatricules) {
            if (matricule == null) continue;

            // Si le matricule actuel est celui du Manager S001, on l'ignore (il reçoit uniquement les copies)
            if (matricule.equalsIgnoreCase(managerMatricule)) continue;

            // Récupération des données de base de l'agent
            Utilisateur baseUser = utilisateurRepository.findByMatricule(matricule);

            if (baseUser == null || baseUser.getEmail() == null) {
                System.err.println("⚠️ Utilisateur introuvable ou email vide pour le matricule : " + matricule);
                continue;
            }

            // Chargement complet de l'utilisateur avec ses collections pour éviter les exceptions de Lazy Loading
            Utilisateur userWithCollections = utilisateurRepository.findByEmailWithCollections(baseUser.getEmail());
            if (userWithCollections == null) {
                userWithCollections = baseUser;
            }

            try {
                // 4. Génération du rapport PDF individuel basé sur les données de cet agent
                byte[] pdf = generateWeeklyPdf(userWithCollections);

                if (pdf != null) {
                    // A) Envoi du rapport par email à l'agent concerné
                    emailService.sendReportEmail(userWithCollections.getEmail(), "Votre Rapport Hebdomadaire d'Activité", pdf);
                    System.out.println("✅ [Agent] Rapport envoyé à : " + userWithCollections.getMatricule());

                    // B) Envoi d'une copie conforme du même rapport au Manager S001
                    if (managerEmail != null && !managerEmail.trim().isEmpty()) {
                        String subjectForManager = "Rapport Hebdomadaire de l'Agent : " + userWithCollections.getNom() + " " + userWithCollections.getPrenom();
                        emailService.sendReportEmail(managerEmail, subjectForManager, pdf);
                        System.out.println("✅ [Manager Copy] Copie du rapport de " + userWithCollections.getMatricule() + " envoyée au Manager S001 (" + managerEmail + ")");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la génération/envoi pour le matricule " + matricule + " : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Génère le fichier PDF contenant le rapport hebdomadaire d'un utilisateur donné.
     * * @param utilisateur L'utilisateur (agent) pour lequel générer le rapport.
     * @return Le tableau d'octets (byte[]) représentant le fichier PDF, ou null si aucun rapport.
     */
    public byte[] generateWeeklyPdf(Utilisateur utilisateur) {
        // Définition de la plage de dates : du lundi en cours à 00:00 jusqu'à l'instant présent
        LocalDateTime startOfWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();

        // Filtrage des actions créées uniquement par cet agent durant la semaine en cours
        List<Report> weeklyActions = reportRepository.findByCreationDateBetween(startOfWeek, LocalDateTime.now()).stream()
                .filter(r -> utilisateur.getMatricule().equals(r.getCreatedBy()))
                .collect(Collectors.toList());

        if (weeklyActions.isEmpty()) return null;

        // Transformation et regroupement des entités Report vers le format DTO
        List<ReportDTO> uniqueActionDTOs = processReportsToDTO(weeklyActions);

        // Extraction des statistiques globales et hebdomadaires de l'agent via le Dashboard Service
        RepoDashboardDTO stats = repoDashboardService.getRecouvreurStats(utilisateur, null);

        // Initialisation du contexte Thymeleaf pour l'injection des variables dans le template HTML
        Context context = new Context();

        // 1. Injection des informations personnelles et professionnelles de l'agent
        context.setVariable("nom", utilisateur.getNom() + " " + utilisateur.getPrenom());
        context.setVariable("matricule", utilisateur.getMatricule());
        context.setVariable("email", utilisateur.getEmail());
        context.setVariable("dateGeneration", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Gestion sécurisée de l'énumération Structure et du libellé du profil
        String structureStr = "-";
        String profilStr = "-";
        if (utilisateur.getProfil() != null) {
            if (utilisateur.getProfil().getStructure() != null) {
                structureStr = utilisateur.getProfil().getStructure().name();
            }
            profilStr = utilisateur.getProfil().getLibelle() != null ? utilisateur.getProfil().getLibelle() : "-";
        }
        context.setVariable("structure", structureStr);
        context.setVariable("libelleProfil", profilStr);

        // 2. Conversion et concaténation des ensembles de codes (Collections/Sets) en chaînes de caractères
        context.setVariable("codesRegions", safeJoinCollection(utilisateur.getRegions()));
        context.setVariable("codesZones", safeJoinCollection(utilisateur.getZones()));
        context.setVariable("codesAgences", safeJoinCollection(utilisateur.getAgences()));
        context.setVariable("codesActivites", safeJoinCollection(utilisateur.getActivites()));
        context.setVariable("codesPaliers", safeJoinCollection(utilisateur.getPaliers()));
        context.setVariable("codesSegments", safeJoinCollection(utilisateur.getSegments()));
        context.setVariable("codesMarches", safeJoinCollection(utilisateur.getMarches()));
        context.setVariable("codesCentres", safeJoinCollection(utilisateur.getCentreAffaires()));

        // 3. Passage de la liste des actions traitées et des indicateurs de performance (KPIs)
        context.setVariable("actions", uniqueActionDTOs);

        // Statistiques globales (Historique complet)
        context.setVariable("totalClientsGlobal", stats.getTotalClientsGlobal());
        context.setVariable("totalPaidGlobal", stats.getTotalPaidGlobal());
        context.setVariable("totalRemainingGlobal", stats.getTotalRemainingGlobal());
        context.setVariable("totalReportsGlobal", stats.getTotalReportsGlobal());

        // Statistiques de la semaine en cours
        context.setVariable("totalClientsWeekly", stats.getTotalClientsWeekly());
        context.setVariable("totalPaidWeekly", stats.getTotalPaidWeekly());
        context.setVariable("totalReportsWeekly", stats.getTotalReportsWeekly());

        // Rendu final en format PDF
        return renderPdf(context);
    }

    /**
     * Convertit une collection d'objets métiers en une chaîne de codes séparés par des virgules.
     * Évite les erreurs de type et gère les valeurs nulles ou vides de façon sécurisée.
     */
    private String safeJoinCollection(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) return "-";
        return collection.stream()
                .map(item -> {
                    if (item instanceof Region) return ((Region) item).getCode();
                    if (item instanceof Zone) return ((Zone) item).getCode();
                    if (item instanceof Agence) return ((Agence) item).getCode();
                    if (item instanceof Activite) return ((Activite) item).getCode();
                    if (item instanceof Palier) return ((Palier) item).getCode();
                    if (item instanceof Segment) return ((Segment) item).getCode();
                    if (item instanceof Marche) return ((Marche) item).getCode();
                    if (item instanceof CentreAffaire) return ((CentreAffaire) item).getCode();
                    return item.toString();
                })
                .filter(code -> code != null && !code.trim().isEmpty())
                .collect(Collectors.joining(", "));
    }

    /**
     * Traite les rapports, les convertit en DTO, effectue un regroupement (Map) par client unique (CLI),
     * cumule les montants payés si un client apparaît plusieurs fois, et trie le tout par date de paiement décroissante.
     */
    private List<ReportDTO> processReportsToDTO(List<Report> reports) {
        return reports.stream()
                .map(report -> reportService.processAndConvertToDTO(report, true))
                .collect(Collectors.toMap(
                        ReportDTO::getCli, dto -> dto,
                        (existing, replacement) -> {
                            // Cumul des montants payés pour le même client unique (CLI)
                            existing.setPaidAmount((existing.getPaidAmount() != null ? existing.getPaidAmount() : 0.0)
                                    + (replacement.getPaidAmount() != null ? replacement.getPaidAmount() : 0.0));
                            return existing;
                        }
                )).values().stream()
                .sorted(Comparator.comparing(ReportDTO::getLastPaymentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Transforme le template Thymeleaf HTML traité en document binaire PDF en utilisant le moteur Flying Saucer (ITextRenderer).
     */
    private byte[] renderPdf(Context context) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Traitement du fichier HTML 'report-template.html' avec les variables du contexte
            String html = templateEngine.process("report-template", context);

            // Initialisation du convertisseur PDF Flying Saucer
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du rendu et de la génération binaire du PDF : " + e.getMessage());
        }
    }
}