package com.kronos.olympus.model.enums;

import java.util.Arrays;
import java.util.List;

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

    public double referenceFor(Gender gender) {
        return gender == Gender.FEMALE ? referenceFemale : referenceMale;
    }

    public static List<Nutrient> ordered() {
        return Arrays.asList(values());
    }
}
