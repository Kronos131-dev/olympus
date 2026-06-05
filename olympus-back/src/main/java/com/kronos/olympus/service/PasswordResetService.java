package com.kronos.olympus.service;

import com.kronos.olympus.model.PasswordResetToken;
import com.kronos.olympus.model.User;
import com.kronos.olympus.repository.PasswordResetTokenRepository;
import com.kronos.olympus.repository.RefreshTokenRepository;
import com.kronos.olympus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Réinitialisation de mot de passe par email (calquée sur la fonctionnalité Chiron).
 * Émet un token opaque à usage unique, l'envoie par email, puis le consomme au reset.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    // Durée de validité du lien : 1 heure.
    private static final long TOKEN_TTL_MINUTES = 60;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${olympus.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Démarre une réinitialisation : envoie un lien à l'email de récupération s'il correspond
     * à un compte. Ne révèle jamais si l'email existe (réponse identique dans tous les cas).
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByRecoveryEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        // Invalide les demandes précédentes non consommées.
        tokenRepository.deleteByUserAndUsedFalse(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .token(generateOpaqueToken())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES))
                .used(false)
                .build();
        tokenRepository.save(token);

        String baseUrl = frontendUrl.replaceAll("/+$", "");
        String resetLink = baseUrl + "/reset-password?token=" + token.getToken();
        try {
            emailService.sendPasswordResetEmail(email, resetLink);
        } catch (Exception ex) {
            log.warn("Échec de l'envoi de l'email de réinitialisation pour {} : {}", email, ex.getMessage());
        }
    }

    /**
     * Consomme un token et applique le nouveau mot de passe. Révoque les sessions en cours.
     * Lève {@link IllegalArgumentException} (HTTP 400) si le token est invalide, expiré ou déjà utilisé.
     */
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Lien de réinitialisation invalide."));
        if (Boolean.TRUE.equals(token.getUsed())) {
            throw new IllegalArgumentException("Ce lien a déjà été utilisé.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ce lien a expiré. Demande un nouveau lien.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        // Sécurité : invalide toutes les sessions (refresh tokens) existantes de l'utilisateur.
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
