package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Poids poussé par une application tierce liée (ex. Chiron) via le token
 * d'intégration. Seul le poids est synchronisable : surface d'écriture minimale.
 */
@Data
public class WeightSyncRequest {

    @NotNull
    @DecimalMin(value = "20.0", message = "Poids trop faible")
    @DecimalMax(value = "500.0", message = "Poids trop élevé")
    private Double weightKg;
}
