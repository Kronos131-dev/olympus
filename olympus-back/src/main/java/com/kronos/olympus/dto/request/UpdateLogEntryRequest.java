package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLogEntryRequest {

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être supérieure à zéro")
    private Double quantityGrams;

    private String unit;
    private Double amount;
}
