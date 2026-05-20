package com.kronos.olympus.model.enums;

public enum ActivityLevel {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    INTENSE(1.725);

    public final double multiplier;
    ActivityLevel(double multiplier) { 
        this.multiplier = multiplier; 
    }
}