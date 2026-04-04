package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPresetRequest {

    @NotBlank(message = "Le nom du repas est obligatoire")
    private String name;

    @NotEmpty(message = "Un repas doit contenir au moins un ingrédient")
    private List<MealIngredientRequest> ingredients;
}
