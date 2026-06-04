package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.IntegrationLinkRequest;
import com.kronos.olympus.dto.request.WeightSyncRequest;
import com.kronos.olympus.dto.response.AuthResponse;
import com.kronos.olympus.dto.response.IntegrationLinkResponse;
import com.kronos.olympus.service.AuthService;
import com.kronos.olympus.service.IntegrationLinkService;
import com.kronos.olympus.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints de liaison de comptes pour l'application tierce Chiron.
 *
 * <p>Flux : Chiron envoie une seule fois les identifiants Olympus de l'utilisateur à
 * {@code POST /link} et reçoit un {@code linkToken} permanent. Ce token, présenté
 * ensuite dans l'en-tête {@code X-Integration-Token}, donne un accès en lecture seule
 * aux endpoints Olympus de l'utilisateur — sans nouvelle authentification, et ce
 * jusqu'à révocation explicite.</p>
 */
@RestController
@RequestMapping("/api/v1/integration/chiron")
@RequiredArgsConstructor
public class IntegrationController {

    private static final String CLIENT = "CHIRON";
    private static final String TOKEN_HEADER = "X-Integration-Token";

    private final IntegrationLinkService integrationLinkService;
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UserService userService;

    /** Crée le lien permanent après vérification des identifiants Olympus. */
    @PostMapping("/link")
    public ResponseEntity<IntegrationLinkResponse> link(@Valid @RequestBody IntegrationLinkRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(integrationLinkService.createLink(request.getEmail(), CLIENT));
    }

    /**
     * Ouvre une session Olympus complète (JWT + refresh) pour le front PWA à partir d'un
     * token de liaison Chiron actif. Permet l'« entrée directe » sans nouvelle saisie de
     * mot de passe lorsque le compte est déjà lié. Le lien ayant été créé avec les
     * identifiants de l'utilisateur, l'élévation vers un accès complet est légitime.
     */
    @PostMapping("/session")
    public ResponseEntity<AuthResponse> session(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return integrationLinkService.resolveUserEmail(token)
                .map(email -> ResponseEntity.ok(authService.issueTokensFor(email)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Synchronise le poids de l'utilisateur depuis l'app tierce liée. C'est la seule
     * écriture autorisée via le token d'intégration : l'endpoint résout lui-même le token
     * (comme {@code /session}) et délègue à {@link UserService}, qui historise la métrique
     * du jour. Le filtre {@code X-Integration-Token} (lecture seule) reste inchangé.
     */
    @PostMapping("/weight")
    public ResponseEntity<Void> syncWeight(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody WeightSyncRequest request) {
        return integrationLinkService.resolveUserEmail(token)
                .map(email -> {
                    userService.updateWeightByEmail(email, request.getWeightKg());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Indique si le token de liaison fourni est encore actif. */
    @GetMapping("/link/status")
    public ResponseEntity<Map<String, Boolean>> status(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return ResponseEntity.ok(Map.of("linked", integrationLinkService.isActive(token)));
    }

    /** Révoque le lien : l'application tierce perd l'accès jusqu'à une nouvelle liaison. */
    @DeleteMapping("/link")
    public ResponseEntity<Void> unlink(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        integrationLinkService.revoke(token);
        return ResponseEntity.noContent().build();
    }
}
