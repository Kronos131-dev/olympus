package com.kronos.olympus.dto.request;

import com.kronos.olympus.model.enums.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanRequest {

    @NotBlank(message = "Le nom du plan est obligatoire")
    private String name;

    @NotNull(message = "Le type de récurrence est obligatoire")
    private RecurrenceType recurrenceType;

    // Requis pour SPECIFIC_WEEKDAYS : DayOfWeek séparés par des virgules (ex: "MONDAY,FRIDAY")
    private String weekdaysMask;

    // Date de référence pour EVERY_OTHER_DAY et CUSTOM (par défaut : aujourd'hui)
    private LocalDate anchorDate;

    // Requis pour CUSTOM : intervalle en jours
    private Integer customIntervalDays;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;

    @NotEmpty(message = "Un plan doit contenir au moins un élément")
    private List<PlannedMealEntryRequest> entries;
}
