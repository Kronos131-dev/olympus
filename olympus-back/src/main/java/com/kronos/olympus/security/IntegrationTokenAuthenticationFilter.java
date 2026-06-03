package com.kronos.olympus.security;

import com.kronos.olympus.service.IntegrationLinkService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentifie les requêtes portant un token de liaison d'intégration permanent
 * (en-tête {@code X-Integration-Token}) comme l'utilisateur Olympus lié.
 *
 * <p>Une fois authentifiée, la requête accède aux endpoints Olympus exactement comme
 * l'utilisateur lui-même. L'accès est volontairement limité aux méthodes de
 * <b>lecture</b> (GET/HEAD) : une application tierce liée ne peut que consulter les
 * données, jamais les modifier.</p>
 */
@Component
@RequiredArgsConstructor
public class IntegrationTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Integration-Token";

    private final IntegrationLinkService integrationLinkService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String token = request.getHeader(HEADER);
        final boolean readOnlyMethod = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());

        if (token != null && !token.isBlank() && readOnlyMethod
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                integrationLinkService.resolveUserEmail(token).ifPresent(email -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            } catch (Exception e) {
                // Token inconnu/révoqué ou utilisateur supprimé : on laisse le contexte
                // vide, Spring Security répondra 401.
                logger.error("Échec de l'authentification par token d'intégration: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
