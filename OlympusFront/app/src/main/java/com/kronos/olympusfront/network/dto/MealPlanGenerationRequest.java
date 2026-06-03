package com.kronos.olympusfront.network.dto;

/** Requête de génération d'un emploi du temps hebdomadaire par l'IA. */
public class MealPlanGenerationRequest {
    private String prompt;

    public MealPlanGenerationRequest() {}

    public MealPlanGenerationRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
