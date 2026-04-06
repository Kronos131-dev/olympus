package com.kronos.olympusfront.network.dto;

public class RegisterRequest {
    private String email;
    private String password;
    private String gender;
    private Double heightCm;
    private Double weightKg;
    private String activityLevel;
    private String goal;

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String gender, Double heightCm, Double weightKg, String activityLevel, String goal) {
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityLevel = activityLevel;
        this.goal = goal;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
}