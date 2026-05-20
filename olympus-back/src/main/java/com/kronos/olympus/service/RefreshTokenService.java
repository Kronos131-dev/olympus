package com.kronos.olympus.service;

import com.kronos.olympus.exception.TokenRefreshException;
import com.kronos.olympus.model.RefreshToken;
import com.kronos.olympus.model.User;
import com.kronos.olympus.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // 30 jours par défaut
    @Value("${application.security.jwt.refresh-expiration:2592000000}")
    private long refreshExpirationMs;

    /**
     * Génère et persiste un nouveau refresh token opaque pour l'utilisateur.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(generateOpaqueToken())
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Vérifie un refresh token et le fait tourner : l'ancien est révoqué, un nouveau est émis.
     * Lève {@link TokenRefreshException} (HTTP 401) si le token est absent, révoqué ou expiré.
     */
    @Transactional
    public RefreshToken verifyAndRotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new TokenRefreshException("Refresh token invalide. Veuillez vous reconnecter."));

        if (Boolean.TRUE.equals(current.getRevoked())) {
            // Réutilisation d'un token déjà tourné : signe possible de vol.
            log.warn("Réutilisation d'un refresh token révoqué détectée pour l'utilisateur {}", current.getUser().getId());
            throw new TokenRefreshException("Refresh token révoqué. Veuillez vous reconnecter.");
        }

        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("Refresh token expiré. Veuillez vous reconnecter.");
        }

        RefreshToken rotated = createRefreshToken(current.getUser());
        current.setRevoked(true);
        current.setReplacedByToken(rotated.getToken());
        refreshTokenRepository.save(current);

        return rotated;
    }

    /**
     * Révoque tous les refresh tokens de l'utilisateur propriétaire du token fourni.
     * Idempotent : ne lève rien si le token est inconnu (logout résilient).
     */
    @Transactional
    public void revokeByToken(String rawToken) {
        refreshTokenRepository.findByToken(rawToken).ifPresent(rt ->
                refreshTokenRepository.revokeAllByUserId(rt.getUser().getId()));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
