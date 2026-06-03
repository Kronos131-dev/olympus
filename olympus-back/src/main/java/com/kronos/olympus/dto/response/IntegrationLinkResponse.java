package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat d'une liaison d'intégration : le {@code linkToken} permanent à conserver
 * par l'application tierce, et l'identité du compte Olympus lié.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationLinkResponse {
    // Token de liaison permanent : à présenter dans l'en-tête X-Integration-Token.
    private String linkToken;
    private Long olympusUserId;
    private String olympusUsername;
}
