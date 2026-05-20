package com.kronos.olympusfront.network.dto;

public class AiMealRequest {
    private String description;

    public AiMealRequest() {}

    public AiMealRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}