package com.kronos.olympus.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filet de sécurité autour de la chaîne de filtres.
 *
 * <p>SecurityConfig construit son DaoAuthenticationProvider par constructeur vide suivi de
 * setUserDetailsService, forme retirée dans Spring Security 7. Ces tests décrivent le
 * comportement observable de la chaîne — qui est ouvert, qui est fermé — pour qu'une
 * reconstruction du provider ne puisse ni ouvrir une route protégée ni fermer la white-list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void authenticationProvider_isWiredWithAUserDetailsService() {
        assertNotNull(authenticationProvider);
        assertNotNull(userDetailsService);
    }

    @Test
    void whiteListedLogin_isReachedWithoutAToken() throws Exception {
        int status = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        assertNotEquals(401, status, "/api/v1/auth/** doit rester ouvert : le contrôleur doit être atteint");
        assertNotEquals(403, status, "/api/v1/auth/** doit rester ouvert : le contrôleur doit être atteint");
    }

    @Test
    void protectedProfileRoute_withoutToken_isRefused() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedDailyLogsRoute_withoutToken_isRefused() throws Exception {
        mockMvc.perform(get("/api/v1/daily-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRoute_withoutToken_isRefusedRatherThanExposed() throws Exception {
        mockMvc.perform(get("/api/v1/there-is-no-such-endpoint"))
                .andExpect(status().isUnauthorized());
    }
}
