package com.kronos.olympus.model;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import com.kronos.olympus.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Utilisé comme Pseudo
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private Double heightCm;

    @Column(nullable = false)
    private Double currentWeightKg;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal goal;

    @Builder.Default
    @Column(nullable = false)
    private Boolean autoCalculateTargets = true;

    @Column(name = "manual_target_kcal")
    private Double manualTargetKcal;

    @Column(name = "manual_target_proteins")
    private Double manualTargetProteins;

    @Column(name = "manual_target_carbs")
    private Double manualTargetCarbs;

    @Column(name = "manual_target_fats")
    private Double manualTargetFats;

    // Fournisseur d'IA préféré pour l'agent conversationnel (Mistral par défaut)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private AiProvider aiProvider = AiProvider.MISTRAL;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.autoCalculateTargets == null) {
            this.autoCalculateTargets = true;
        }
        if (this.aiProvider == null) {
            this.aiProvider = AiProvider.MISTRAL;
        }
        if (this.birthDate == null) {
            // Fallback pour les utilisateurs existants
            this.birthDate = LocalDate.now().minusYears(25);
        }
    }
}
