package com.kronos.olympusfront.network.dto;

public class UpdateActivityRequest {
    private String targetDate;
    private Integer stepCount;
    private Integer workoutDurationMinutes;
    private Integer manualKcalBurned;

    public UpdateActivityRequest() {}

    public UpdateActivityRequest(String targetDate, Integer stepCount, Integer workoutDurationMinutes, Integer manualKcalBurned) {
        this.targetDate = targetDate;
        this.stepCount = stepCount;
        this.workoutDurationMinutes = workoutDurationMinutes;
        this.manualKcalBurned = manualKcalBurned;
    }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    public Integer getWorkoutDurationMinutes() { return workoutDurationMinutes; }
    public void setWorkoutDurationMinutes(Integer workoutDurationMinutes) { this.workoutDurationMinutes = workoutDurationMinutes; }
    public Integer getManualKcalBurned() { return manualKcalBurned; }
    public void setManualKcalBurned(Integer manualKcalBurned) { this.manualKcalBurned = manualKcalBurned; }
}