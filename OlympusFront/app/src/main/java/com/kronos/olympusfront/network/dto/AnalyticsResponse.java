package com.kronos.olympusfront.network.dto;

import java.util.List;

public class AnalyticsResponse {
    private String startDate;
    private String endDate;
    private Double averageKcal;
    private Double averageWeight;
    private Double estimatedFatLossGrams;
    private List<DailyMetricPoint> dailyData;

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Double getAverageKcal() { return averageKcal; }
    public void setAverageKcal(Double averageKcal) { this.averageKcal = averageKcal; }
    public Double getAverageWeight() { return averageWeight; }
    public void setAverageWeight(Double averageWeight) { this.averageWeight = averageWeight; }
    public Double getEstimatedFatLossGrams() { return estimatedFatLossGrams; }
    public void setEstimatedFatLossGrams(Double estimatedFatLossGrams) { this.estimatedFatLossGrams = estimatedFatLossGrams; }
    public List<DailyMetricPoint> getDailyData() { return dailyData; }
    public void setDailyData(List<DailyMetricPoint> dailyData) { this.dailyData = dailyData; }

    public static class DailyMetricPoint {
        private String date;
        private Double weightKg;
        private Double targetKcal;
        private Double totalKcal;
        private Double totalProteins;
        private Double totalCarbs;
        private Double totalFats;
        private Double extraKcalBurned;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getWeightKg() { return weightKg; }
        public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
        public Double getTargetKcal() { return targetKcal; }
        public void setTargetKcal(Double targetKcal) { this.targetKcal = targetKcal; }
        public Double getTotalKcal() { return totalKcal; }
        public void setTotalKcal(Double totalKcal) { this.totalKcal = totalKcal; }
        public Double getTotalProteins() { return totalProteins; }
        public void setTotalProteins(Double totalProteins) { this.totalProteins = totalProteins; }
        public Double getTotalCarbs() { return totalCarbs; }
        public void setTotalCarbs(Double totalCarbs) { this.totalCarbs = totalCarbs; }
        public Double getTotalFats() { return totalFats; }
        public void setTotalFats(Double totalFats) { this.totalFats = totalFats; }
        public Double getExtraKcalBurned() { return extraKcalBurned; }
        public void setExtraKcalBurned(Double extraKcalBurned) { this.extraKcalBurned = extraKcalBurned; }
    }
}