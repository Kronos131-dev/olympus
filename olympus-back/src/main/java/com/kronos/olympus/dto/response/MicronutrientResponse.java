package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.model.enums.NutrientCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Double coverage;
}
