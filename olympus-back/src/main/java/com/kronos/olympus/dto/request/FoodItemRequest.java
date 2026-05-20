package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodItemRequest {
    @NotBlank(message = "Le nom de l'aliment est obligatoire")
    private String name;

    @NotNull(message = "Les calories sont obligatoires")
    private Double kcal100g;

    @NotNull(message = "Les protéines sont obligatoires")
    private Double proteins100g;

    @NotNull(message = "Les glucides sont obligatoires")
    private Double carbs100g;

    @NotNull(message = "Les lipides sont obligatoires")
    private Double fats100g;
}
