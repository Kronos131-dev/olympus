package com.kronos.olympus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Renvoie un 401 Unauthorized (et non le 403 Forbidden par défaut de Spring Security)
 * lorsqu'une requête atteint un endpoint protégé sans authentification valide —
 * typiquement un access token JWT expiré.
 *
 * <p>Le client mobile s'appuie sur ce code 401 précis : son {@code Authenticator}
 * OkHttp ne se déclenche que sur un 401 pour rafraîchir automatiquement l'access
 * token via le refresh token. Avec le 403 par défaut, le rafraîchissement n'avait
 * jamais lieu et l'utilisateur était déconnecté à chaque expiration du token.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "detail", "Authentification requise ou token expiré."
        ));
    }
}
