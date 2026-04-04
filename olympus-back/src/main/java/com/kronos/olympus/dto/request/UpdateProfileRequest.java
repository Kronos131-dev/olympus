package com.kronos.olympus.dto.request;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.Goal;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    @Positive(message = "Le poids doit être positif")
    private Double weightKg;

    private ActivityLevel activityLevel;

    private Goal goal;
}
