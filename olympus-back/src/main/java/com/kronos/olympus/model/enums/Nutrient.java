package com.kronos.olympus.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Micronutriments suivis par l'application, avec leur colonne dans ciqual.csv et les Références
 * Nutritionnelles pour la Population publiées par l'ANSES.
 *
 * <p>Les quotas vivent ici et non dans le CSV : ils dépendent de la personne (sexe, et plus tard
 * l'âge), alors que le CSV ne porte que des valeurs pour 100 g d'aliment. Ajouter un micronutriment
 * se résume donc à une constante ici et une colonne dans le CSV.
 *
 * <p>Les macronutriments et le palier « fibres, sucres, AG saturés, sel » n'y figurent pas : eux
 * sont des colonnes explicites de {@link com.kronos.olympus.model.FoodItem}, parce qu'Open Food
 * Facts les fournit aussi et qu'ils peuvent donc être totalisés de façon fiable sur une journée.
 */
public enum Nutrient {

    CALCIUM("calcium_mg", "mg", NutrientCategory.MINERAL, 950, 950),
    IRON("fer_mg", "mg", NutrientCategory.MINERAL, 11, 16),
    MAGNESIUM("magnesium_mg", "mg", NutrientCategory.MINERAL, 380, 300),
    POTASSIUM("potassium_mg", "mg", NutrientCategory.MINERAL, 3500, 3500),
    ZINC("zinc_mg", "mg", NutrientCategory.MINERAL, 14, 11),
    SELENIUM("selenium_ug", "µg", NutrientCategory.MINERAL, 70, 70),
    IODINE("iode_ug", "µg", NutrientCategory.MINERAL, 150, 150),

    VITAMIN_A("vit_a_er_ug", "µg", NutrientCategory.VITAMIN, 750, 650),
    VITAMIN_C("vit_c_mg", "mg", NutrientCategory.VITAMIN, 110, 110),
    VITAMIN_D("vit_d_ug", "µg", NutrientCategory.VITAMIN, 15, 15),
    VITAMIN_B9("vit_b9_ug", "µg", NutrientCategory.VITAMIN, 330, 330),
    VITAMIN_B12("vit_b12_ug", "µg", NutrientCategory.VITAMIN, 4, 4),

    OMEGA3_ALA("omega3_ala_g", "g", NutrientCategory.FATTY_ACID, 2.8, 2.2),
    OMEGA3_EPA_DHA("omega3_epadha_g", "g", NutrientCategory.FATTY_ACID, 0.5, 0.5);

    private final String csvColumn;
    private final String unit;
    private final NutrientCategory category;
    private final double referenceMale;
    private final double referenceFemale;

    Nutrient(String csvColumn, String unit, NutrientCategory category,
             double referenceMale, double referenceFemale) {
        this.csvColumn = csvColumn;
        this.unit = unit;
        this.category = category;
        this.referenceMale = referenceMale;
        this.referenceFemale = referenceFemale;
    }

    public String getCsvColumn() {
        return csvColumn;
    }

    public String getUnit() {
        return unit;
    }

    public NutrientCategory getCategory() {
        return category;
    }

    /** Apport quotidien de référence pour ce nutriment, selon le sexe. */
    public double referenceFor(Gender gender) {
        return gender == Gender.FEMALE ? referenceFemale : referenceMale;
    }

    public static List<Nutrient> ordered() {
        return Arrays.asList(values());
    }
}
