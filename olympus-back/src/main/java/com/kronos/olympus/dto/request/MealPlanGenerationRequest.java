package com.kronos.olympus.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête de génération d'un emploi du temps hebdomadaire par l'IA.
 * Le {@code prompt} décrit les préférences (objectif, allergies, budget…) ; il peut être vide.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanGenerationRequest {
    private String prompt;
}
