package com.example.backend.Repository;

import com.example.backend.Model.Profil;
import com.example.backend.Model.Utilisateur;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    Utilisateur findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMatricule(String matricule);

    Utilisateur findByMatricule(String matricule);
    boolean existsByProfilId(UUID id);
    Optional<Utilisateur> findByActivationToken(String token);


    List<Utilisateur> findByEnabledFalseAndCreatedAtBefore(LocalDateTime dateTime);
    // ← الـ method الجديدة بـ JOIN FETCH
    @Query("""
            SELECT u FROM Utilisateur u
            LEFT JOIN FETCH u.activites
            LEFT JOIN FETCH u.agences
            LEFT JOIN FETCH u.zones
            LEFT JOIN FETCH u.regions
            LEFT JOIN FETCH u.paliers
            LEFT JOIN FETCH u.marches
            LEFT JOIN FETCH u.segments
            LEFT JOIN FETCH u.centreAffaires
            WHERE u.email = :email
        """)
    Utilisateur findByEmailWithCollections(@Param("email") String email);


}
