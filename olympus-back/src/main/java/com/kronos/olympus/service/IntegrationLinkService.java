package com.kronos.olympus.service;

import com.kronos.olympus.dto.response.IntegrationLinkResponse;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.model.IntegrationLink;
import com.kronos.olympus.model.User;
import com.kronos.olympus.repository.IntegrationLinkRepository;
import com.kronos.olympus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Gère les liens d'intégration permanents entre un compte Olympus et une application
 * tierce de confiance (ex. Chiron).
 *
 * <p>Contrairement aux JWT (courts) et aux refresh tokens (rotatifs, 30 jours), le token
 * de liaison ne expire jamais : une fois le compte lié, l'application tierce y accède
 * indéfiniment sans nouvelle authentification, jusqu'à révocation explicite.</p>
 *
 * <p>Ce service ne dépend volontairement <b>pas</b> de l'{@code AuthenticationManager} :
 * il fait partie de la chaîne de construction de {@code SecurityConfig} (via le filtre
 * d'authentification d'intégration). La vérification des identifiants Olympus est donc
 * faite en amont par {@code IntegrationController}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationLinkService {

    private final IntegrationLinkRepository integrationLinkRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Crée (ou remplace) le lien d'intégration permanent du compte Olympus identifié
     * par son email. Les identifiants doivent avoir été vérifiés par l'appelant.
     */
    @Transactional
    public IntegrationLinkResponse createLink(String email, String client) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + email));

        // Un seul lien actif par (utilisateur, client) : on révoque l'éventuel précédent.
        integrationLinkRepository.findFirstByUserIdAndClientAndRevokedFalse(user.getId(), client)
                .ifPresent(existing -> {
                    existing.setRevoked(true);
                    integrationLinkRepository.save(existing);
                });

        IntegrationLink link = integrationLinkRepository.save(IntegrationLink.builder()
                .token(generateOpaqueToken())
                .user(user)
                .client(client)
                .revoked(false)
                .build());
        log.info("INTEGRATION_LINKED client={} userId={}", client, user.getId());

        return IntegrationLinkResponse.builder()
                .linkToken(link.getToken())
                .olympusUserId(user.getId())
                .olympusUsername(user.getEmail())
                .build();
    }

    /**
     * Résout l'email du compte Olympus associé à un token de liaison actif.
     * L'email est lu dans la transaction pour éviter tout souci de lazy loading.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolveUserEmail(String token) {
        return integrationLinkRepository.findByTokenAndRevokedFalse(token)
                .map(link -> link.getUser().getEmail());
    }

    /** Indique si le token de liaison fourni correspond à un lien actif. */
    @Transactional(readOnly = true)
    public boolean isActive(String token) {
        return token != null && !token.isBlank()
                && integrationLinkRepository.findByTokenAndRevokedFalse(token).isPresent();
    }

    /** Révoque le lien correspondant au token (idempotent). */
    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        integrationLinkRepository.findByTokenAndRevokedFalse(token).ifPresent(link -> {
            link.setRevoked(true);
            integrationLinkRepository.save(link);
            log.info("INTEGRATION_UNLINKED client={} userId={}", link.getClient(), link.getUser().getId());
        });
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
