package com.kronos.olympusfront.network.dto;

import java.io.Serializable;
import java.util.List;

public class MealPlanResponse implements Serializable {
    private Long id;
    private String name;
    private String recurrenceType;
    private String weekdaysMask;
    private String anchorDate;
    private Integer customIntervalDays;
    private String startDate;
    private String endDate;
    private Boolean active;
    private List<PlannedMealEntryResponse> entries;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }
    public String getWeekdaysMask() { return weekdaysMask; }
    public void setWeekdaysMask(String weekdaysMask) { this.weekdaysMask = weekdaysMask; }
    public String getAnchorDate() { return anchorDate; }
    public void setAnchorDate(String anchorDate) { this.anchorDate = anchorDate; }
    public Integer getCustomIntervalDays() { return customIntervalDays; }
    public void setCustomIntervalDays(Integer customIntervalDays) { this.customIntervalDays = customIntervalDays; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public List<PlannedMealEntryResponse> getEntries() { return entries; }
    public void setEntries(List<PlannedMealEntryResponse> entries) { this.entries = entries; }
}
