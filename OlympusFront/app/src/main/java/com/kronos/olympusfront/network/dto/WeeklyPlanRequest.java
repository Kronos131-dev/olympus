package com.kronos.olympusfront.network.dto;

import java.util.List;

/** Requête d'enregistrement de l'emploi du temps hebdomadaire (liste plate des repas planifiés). */
public class WeeklyPlanRequest {
    private List<PlannedMealEntryRequest> entries;

    public WeeklyPlanRequest() {}

    public WeeklyPlanRequest(List<PlannedMealEntryRequest> entries) {
        this.entries = entries;
    }

    public List<PlannedMealEntryRequest> getEntries() { return entries; }
    public void setEntries(List<PlannedMealEntryRequest> entries) { this.entries = entries; }
}
