package com.kronos.olympus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identifiants Olympus fournis par une application tierce pour créer un lien
 * d'intégration permanent.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationLinkRequest {

    @NotBlank(message = "Le pseudo est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
