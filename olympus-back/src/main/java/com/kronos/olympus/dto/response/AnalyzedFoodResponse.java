package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Nutrient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Un aliment reconnu sur la photo, déjà mis à l'échelle de la quantité estimée.
 *
 * <p>{@code source} dit d'où viennent les chiffres : {@code CIQUAL} pour une correspondance dans
 * la table de l'ANSES, {@code AI} lorsque le modèle a dû les estimer lui-même — auquel cas
 * {@code micros} est vide, et l'écran doit le montrer plutôt que de laisser croire à des zéros.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedFoodResponse {

    private String name;
    private Double quantityGrams;
    private FoodSource source;

    /** Renseigné quand l'aliment vient du référentiel ; null pour une estimation pure du modèle. */
    private Long foodItemId;

    private Double kcal;
    private Double proteins;
    private Double carbs;
    private Double fats;

    private Double fibers;
    private Double sugars;
    private Double saturatedFat;
    private Double salt;

    @Builder.Default
    private Map<Nutrient, Double> micros = Map.of();
}
