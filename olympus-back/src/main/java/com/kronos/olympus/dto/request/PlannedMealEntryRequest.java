package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedMealEntryRequest {

    // Soit foodItemId, soit mealPresetId doit être renseigné (et un seul)
    private Long foodItemId;

    private Long mealPresetId;

    @Positive(message = "La quantité doit être positive")
    private Double quantityGrams;

    // Heure prévue (optionnelle), ex: "08:30"
    private LocalTime plannedTime;

    // Jour de la semaine (plan hebdomadaire) : MONDAY, TUESDAY, ...
    private DayOfWeek dayOfWeek;
}
