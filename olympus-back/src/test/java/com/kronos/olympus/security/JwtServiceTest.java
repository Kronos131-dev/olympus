package com.kronos.olympus.security;

import com.kronos.olympus.model.enums.Role;
import com.kronos.olympus.model.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filet de sécurité autour de l'émission et de la validation des jetons.
 *
 * <p>JwtService est écrit contre l'API jjwt 0.11 ({@code parserBuilder}, {@code setSigningKey},
 * {@code SignatureAlgorithm}), supprimée en 0.12. Ces tests décrivent le comportement attendu
 * indépendamment de l'API employée : ils doivent rester verts après la réécriture.
 */
class JwtServiceTest {

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static final String OTHER_SECRET =
            "586E3272357538782F413F4428472B4B6250645367566B5970404E6352665655";

    private JwtService jwtService;

    private UserDetailsImpl athlete;

    @BeforeEach
    void setUp() {
        jwtService = buildService(SECRET, 900_000L);

        User user = new User();
        user.setId(42L);
        user.setEmail("athlete@olympus.app");
        user.setPasswordHash("hash");
        user.setRole(Role.USER);
        athlete = new UserDetailsImpl(user);
    }

    private JwtService buildService(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "jwtExpiration", expirationMs);
        return service;
    }

    @Test
    void generateToken_thenExtractUsername_returnsTheSubject() {
        String token = jwtService.generateToken(athlete);

        assertEquals("athlete@olympus.app", jwtService.extractUsername(token));
    }

    @Test
    void generateToken_embedsUserIdAndRoleClaims() {
        String token = jwtService.generateToken(athlete);

        Number userId = jwtService.extractClaim(token, claims -> claims.get("userId", Number.class));
        assertEquals(42L, userId.longValue());
        assertEquals("USER", jwtService.extractClaim(token, claims -> claims.get("role", String.class)));
    }

    @Test
    void isTokenValid_matchingUser_returnsTrue() {
        String token = jwtService.generateToken(athlete);

        assertTrue(jwtService.isTokenValid(token, athlete));
    }

    @Test
    void isTokenValid_otherUser_returnsFalse() {
        String token = jwtService.generateToken(athlete);

        User other = new User();
        other.setId(7L);
        other.setEmail("someone.else@olympus.app");
        other.setPasswordHash("hash");
        other.setRole(Role.USER);

        assertFalse(jwtService.isTokenValid(token, new UserDetailsImpl(other)));
    }

    @Test
    void expiredToken_isRejected() {
        JwtService shortLived = buildService(SECRET, -1_000L);
        String token = shortLived.generateToken(athlete);

        assertThrows(JwtException.class, () -> shortLived.isTokenValid(token, athlete));
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String foreignToken = buildService(OTHER_SECRET, 900_000L).generateToken(athlete);

        assertThrows(JwtException.class, () -> jwtService.extractUsername(foreignToken));
    }

    @Test
    void expirationClaim_isTheConfiguredLifetimeAhead() {
        String token = jwtService.generateToken(athlete);

        Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
        long remainingMs = expiration.getTime() - System.currentTimeMillis();

        assertTrue(remainingMs > 800_000L && remainingMs <= 900_000L,
                "expiration attendue à ~15 min, obtenue à " + remainingMs + " ms");
    }
}
