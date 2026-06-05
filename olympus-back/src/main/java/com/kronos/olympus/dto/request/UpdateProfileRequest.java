package com.kronos.olympus.dto.request;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.AiProvider;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private Double currentWeightKg;

    // Email de récupération (mot de passe oublié). Optionnel à la mise à jour.
    @Email(message = "Email invalide")
    private String recoveryEmail;

    private Double heightCm;
    private Gender gender;
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