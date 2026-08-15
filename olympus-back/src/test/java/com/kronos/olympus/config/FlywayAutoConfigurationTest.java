package com.kronos.olympus.config;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou sur la présence de Flyway et de son auto-configuration.
 *
 * <p>Spring Boot 4 a sorti Flyway de l'auto-configuration du cœur : sans la dépendance explicite
 * {@code spring-boot-starter-flyway}, le bean disparaît et les migrations ne s'exécutent plus —
 * sans erreur, sans log, jusqu'à ce qu'une colonne manque en production.
 *
 * <p>Le contrôle est volontairement fait sur le classpath plutôt que sur un contexte Spring :
 * le profil de test désactive Flyway et bâtit le schéma avec Hibernate, donc aucun contexte de
 * test n'instancierait le bean. On vérifie ici que l'outil est là et déclaré, ce qui est
 * exactement ce que la migration risque de perdre.
 */
class FlywayAutoConfigurationTest {

    @Test
    void flywayCore_isOnTheClasspath() {
        assertDoesNotThrow(() -> Class.forName("org.flywaydb.core.Flyway"),
                "flyway-core absent : plus aucune migration ne peut s'exécuter");
    }

    @Test
    void aFlywayAutoConfiguration_isRegistered() throws Exception {
        String registry = readAutoConfigurationRegistry();

        assertTrue(registry.contains("flyway"),
                "aucune auto-configuration Flyway déclarée : depuis Spring Boot 4 il faut la "
                        + "dépendance spring-boot-starter-flyway, sinon les migrations ne tournent "
                        + "jamais au démarrage");
    }

    private String readAutoConfigurationRegistry() throws Exception {
        Enumeration<URL> resources = getClass().getClassLoader().getResources(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        StringBuilder all = new StringBuilder();
        while (resources.hasMoreElements()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resources.nextElement().openStream(), StandardCharsets.UTF_8))) {
                all.append(reader.lines().collect(Collectors.joining("\n")).toLowerCase()).append('\n');
            }
        }
        return all.toString();
    }
}
