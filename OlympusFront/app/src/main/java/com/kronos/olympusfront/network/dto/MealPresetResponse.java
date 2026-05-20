package com.kronos.olympusfront.network.dto;

import java.io.Serializable;
import java.util.List;

public class MealPresetResponse implements Serializable {
    private Long id;
    private String name;
    private List<MealIngredientResponse> ingredients;
    private Double totalKcal;
    private Double totalProteins;
    private Double totalCarbs;
    private Double totalFats;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<MealIngredientResponse> getIngredients() { return ingredients; }
    public void setIngredients(List<MealIngredientResponse> ingredients) { this.ingredients = ingredients; }
    public Double getTotalKcal() { return totalKcal; }
    public void setTotalKcal(Double totalKcal) { this.totalKcal = totalKcal; }
    public Double getTotalProteins() { return totalProteins; }
    public void setTotalProteins(Double totalProteins) { this.totalProteins = totalProteins; }
    public Double getTotalCarbs() { return totalCarbs; }
    public void setTotalCarbs(Double totalCarbs) { this.totalCarbs = totalCarbs; }
    public Double getTotalFats() { return totalFats; }
    public void setTotalFats(Double totalFats) { this.totalFats = totalFats; }
}