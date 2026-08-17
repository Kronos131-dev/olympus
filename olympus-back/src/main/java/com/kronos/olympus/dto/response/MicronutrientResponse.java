package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.model.enums.NutrientCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Apport d'un micronutriment sur la journée, rapporté à la référence ANSES de l'utilisateur. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicronutrientResponse {

    private Nutrient nutrient;
    private NutrientCategory category;
    private String unit;
    private Double consumed;
    private Double reference;

    /**
     * Part des calories du jour provenant d'aliments dont ce nutriment est renseigné, entre 0 et 1.
     *
     * <p>Sans elle, une journée où l'on a scanné un produit industriel — qui ne porte aucun
     * micronutriment — afficherait une carence qui n'existe pas. L'écran doit relativiser
     * l'apport affiché à hauteur de cette couverture.
     */
    private Double coverage;
}
