package com.kronos.olympusfront.network.dto;

public class UserResponse {
    private Long id;
    private String email;
    private String gender;
    private Double heightCm;
    private Double currentWeightKg;
    private String activityLevel;
    private String goal;
    
    private Boolean autoCalculateTargets;
    private Double manualTargetKcal;
    private Double manualTargetProteins;
    private Double manualTargetCarbs;
    private Double manualTargetFats;
    
    // Cibles nutritionnelles calculées par le backend
    private Double targetKcal;
    private Double targetProteins;
    private Double targetCarbs;
    private Double targetFats;
    
    private String createdAt;

    // Fournisseur d'IA préféré pour l'agent ("MISTRAL" / "GEMINI")
    private String aiProvider;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getCurrentWeightKg() { return currentWeightKg; }
    public void setCurrentWeightKg(Double currentWeightKg) { this.currentWeightKg = currentWeightKg; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    
    public Boolean getAutoCalculateTargets() { return autoCalculateTargets; }
    public void setAutoCalculateTargets(Boolean autoCalculateTargets) { this.autoCalculateTargets = autoCalculateTargets; }
    public Double getManualTargetKcal() { return manualTargetKcal; }
    public void setManualTargetKcal(Double manualTargetKcal) { this.manualTargetKcal = manualTargetKcal; }
    public Double getManualTargetProteins() { return manualTargetProteins; }
    public void setManualTargetProteins(Double manualTargetProteins) { this.manualTargetProteins = manualTargetProteins; }
    public Double getManualTargetCarbs() { return manualTargetCarbs; }
    public void setManualTargetCarbs(Double manualTargetCarbs) { this.manualTargetCarbs = manualTargetCarbs; }
    public Double getManualTargetFats() { return manualTargetFats; }
    public void setManualTargetFats(Double manualTargetFats) { this.manualTargetFats = manualTargetFats; }

    public Double getTargetKcal() { return targetKcal; }
    public void setTargetKcal(Double targetKcal) { this.targetKcal = targetKcal; }
    public Double getTargetProteins() { return targetProteins; }
    public void setTargetProteins(Double targetProteins) { this.targetProteins = targetProteins; }
    public Double getTargetCarbs() { return targetCarbs; }
    public void setTargetCarbs(Double targetCarbs) { this.targetCarbs = targetCarbs; }
    public Double getTargetFats() { return targetFats; }
    public void setTargetFats(Double targetFats) { this.targetFats = targetFats; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
}