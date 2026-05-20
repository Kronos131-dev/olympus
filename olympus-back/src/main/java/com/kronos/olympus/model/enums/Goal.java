package com.kronos.olympus.model.enums;

public enum Goal {
    LOSE_WEIGHT(0.85, 2.0),
    MAINTAIN(1.0, 1.6),
    GAIN_MUSCLE(1.10, 1.8);

    public final double calorieModifier;
    public final double proteinPerKg;

    Goal(double calorieModifier, double proteinPerKg) {
        this.calorieModifier = calorieModifier;
        this.proteinPerKg = proteinPerKg;
    }
}