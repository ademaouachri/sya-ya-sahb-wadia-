package com.example.backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "REPORT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    // --- Champs de la première capture (Partie Gauche) ---

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "CLI")
    private String cli; // Numéro client

    @Column(name = "POINT")
    private String point;

    @Column(name = "INCIDENT_REASON")
    private String incidentReason;

    @Column(name = "ESTIMATION")
    private String estimation;

    @Column(name = "CONTACTED_BY")
    private String contactedBy;

    @Column(name = "OBSERVATION", length = 1000)
    private String observation;

    @Column(name = "AMOUNT")
    private Double amount;

    @Column(name = "LOCATION")
    private String location;

    // --- Champs de la seconde capture (Partie Droite) ---

    @Column(name = "REPORT", length = 2000)
    private String reportLabel;

    @Column(name = "VISIT_DATE")
    private String visitDate;

    @Column(name = "VISIT_TIME")
    private String visitTime;

    @Column(name = "PHONE1")
    private String phone1;

    @Column(name = "PHONE2")
    private String phone2;

    @Column(name = "FAX")
    private String fax;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private PaymentStatus status;

    @Column(name = "PAID_AMOUNT")
    private Double paidAmount;
    @Column(name = "montant_depassement")
    private Double montantDepassement;

    @Column(name = "SCHEDULE_NUMBER")
    private Integer scheduleNumber;


    @Column(name = "date_d'estination")
    private LocalDateTime date_destination;



    // ✅ CHAMP AJOUTÉ : Date d'engagement (obligatoire pour Promesse et Facilité)
    @Column(name = "ENGAGEMENT_DATE", nullable = true) // لازم تكون true
    private String engagementDate;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("report")
    private List<Echeance> echeances;
}