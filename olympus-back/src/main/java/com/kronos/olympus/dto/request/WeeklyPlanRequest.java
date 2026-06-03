package com.kronos.olympus.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Requête d'enregistrement de l'emploi du temps hebdomadaire : la liste plate des
 * repas planifiés, chacun portant son {@code dayOfWeek}. Une semaine vide est autorisée.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPlanRequest {
    private List<PlannedMealEntryRequest> entries;
}
