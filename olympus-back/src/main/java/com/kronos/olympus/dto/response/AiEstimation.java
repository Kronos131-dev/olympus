package com.kronos.olympus.dto.response;

import lombok.Data;

@Data
public class AiEstimation {
    private String name;
    private Double totalWeightGrams;
    private Double totalKcal;
    private Double totalProteins;
    private Double totalCarbs;
    private Double totalFats;
}
