package com.kronos.olympus.dto.request;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.Goal;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private Double currentWeightKg;
    private Double heightCm;
    private LocalDate birthDate;
    private ActivityLevel activityLevel;
    private Goal goal;
    
    // Pour activer/désactiver le calcul automatique
    private Boolean autoCalculateTargets;
    
    // Si autoCalculateTargets est false, l'utilisateur peut set ces quotas
    private Double manualTargetKcal;
    private Double manualTargetProteins;
    private Double manualTargetCarbs;
    private Double manualTargetFats;

    // Préférence de fournisseur d'IA (MISTRAL / GEMINI)
    private AiProvider aiProvider;
}