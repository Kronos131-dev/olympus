package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String recoveryEmail;
    private Gender gender;
    private Double heightCm;
    private Double currentWeightKg;
    private LocalDate birthDate;
    private ActivityLevel activityLevel;
    private Goal goal;
    
    private Boolean autoCalculateTargets;
    private Double manualTargetKcal;
    private Double manualTargetProteins;
    private Double manualTargetCarbs;
    private Double manualTargetFats;
    
    // Champs pour les cibles nutritionnelles calculées dynamiquement (ou manuelles si auto = false)
    private Double targetKcal;
    private Double targetProteins;
    private Double targetCarbs;
    private Double targetFats;

    private Double targetFibers;

    // Fournisseur d'IA préféré pour l'agent conversationnel
    private AiProvider aiProvider;

    private LocalDateTime createdAt;
}