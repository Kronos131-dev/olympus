package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiMealRequest {
    @NotBlank(message = "La description du repas est obligatoire")
    private String description;
}
