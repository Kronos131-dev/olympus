package com.kronos.olympusfront.network.dto;

import java.util.List;

public class DailyLogResponse {
    private Long id;
    private String targetDate;
    private Double totalKcal;
    private Double totalProteins;
    private Double totalCarbs;
    private Double totalFats;
    private List<LogEntryResponse> entries;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public Double getTotalKcal() { return totalKcal; }
    public void setTotalKcal(Double totalKcal) { this.totalKcal = totalKcal; }
    public Double getTotalProteins() { return totalProteins; }
    public void setTotalProteins(Double totalProteins) { this.totalProteins = totalProteins; }
    public Double getTotalCarbs() { return totalCarbs; }
    public void setTotalCarbs(Double totalCarbs) { this.totalCarbs = totalCarbs; }
    public Double getTotalFats() { return totalFats; }
    public void setTotalFats(Double totalFats) { this.totalFats = totalFats; }
    public List<LogEntryResponse> getEntries() { return entries; }
    public void setEntries(List<LogEntryResponse> entries) { this.entries = entries; }
}