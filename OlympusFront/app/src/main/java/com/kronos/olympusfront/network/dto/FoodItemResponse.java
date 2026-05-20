package com.kronos.olympusfront.network.dto;

public class FoodItemResponse {
    private Long id;
    private String barcode;
    private String name;
    private Double kcal100g;
    private Double proteins100g;
    private Double carbs100g;
    private Double fats100g;
    private String source;
    private Double estimatedWeightGrams; // Nouvel attribut

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getKcal100g() { return kcal100g; }
    public void setKcal100g(Double kcal100g) { this.kcal100g = kcal100g; }
    public Double getProteins100g() { return proteins100g; }
    public void setProteins100g(Double proteins100g) { this.proteins100g = proteins100g; }
    public Double getCarbs100g() { return carbs100g; }
    public void setCarbs100g(Double carbs100g) { this.carbs100g = carbs100g; }
    public Double getFats100g() { return fats100g; }
    public void setFats100g(Double fats100g) { this.fats100g = fats100g; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Double getEstimatedWeightGrams() { return estimatedWeightGrams; }
    public void setEstimatedWeightGrams(Double estimatedWeightGrams) { this.estimatedWeightGrams = estimatedWeightGrams; }
}