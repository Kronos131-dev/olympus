package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityRequest {
    @NotNull(message = "La date est obligatoire")
    private LocalDate targetDate;

    private Integer stepCount;
    private Integer workoutDurationMinutes;
    private Integer manualKcalBurned;
}