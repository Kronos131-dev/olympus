package com.kronos.olympus.service.ai;

import com.kronos.olympus.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Garde-fou des appels aux API LLM externes (Mistral, Gemini), partagé par tous les
 * appelants ({@code MistralAgentClient}, {@code GeminiAgentClient}, {@code AiService}).
 *
 * <p>Deux rôles :</p>
 * <ul>
 *   <li><b>Throttle</b> : garantit un intervalle minimum entre deux appels d'un même
 *   fournisseur. Indispensable avec une clé Mistral gratuite (~1 requête/seconde) pour
 *   ne pas déclencher de 429 en rafale et risquer le blocage de la clé.</li>
 *   <li><b>Retry</b> : sur HTTP 429 (Too Many Requests) et 503 (Service Unavailable),
 *   réessaie avec un back-off exponentiel, en respectant l'en-tête {@code Retry-After}
 *   lorsqu'il est présent et numérique.</li>
 * </ul>
 */
@Component
@Slf4j
public class LlmRateLimiter {

    @Value("${olympus.agent.mistral-min-interval-ms:1100}")
    private long mistralMinIntervalMs;

    @Value("${olympus.agent.gemini-min-interval-ms:1100}")
    private long geminiMinIntervalMs;

    @Value("${olympus.agent.llm-max-retries:3}")
    private int maxRetries;

    /** Verrou de sérialisation par fournisseur. */
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    /** Horodatage (ms) du dernier appel terminé, par fournisseur. */
    private final ConcurrentHashMap<String, Long> lastCallAt = new ConcurrentHashMap<>();

    /**
     * Exécute un appel LLM en respectant la cadence du fournisseur et en réessayant
     * automatiquement sur 429/503.
     *
     * @param provider "mistral" ou "gemini" (clé de throttle)
     * @param call     l'appel HTTP à exécuter
     */
    public <T> T execute(String provider, Supplier<T> call) {
        Object lock = locks.computeIfAbsent(provider, p -> new Object());
        long minInterval = "gemini".equals(provider) ? geminiMinIntervalMs : mistralMinIntervalMs;

        // Le verrou sérialise les appels d'un même fournisseur : la cadence est ainsi
        // respectée même quand plusieurs requêtes utilisateur arrivent en parallèle.
        synchronized (lock) {
            long backoffMs = Math.max(minInterval, 1000);
            for (int attempt = 1; ; attempt++) {
                throttle(provider, minInterval);
                try {
                    T result = call.get();
                    lastCallAt.put(provider, System.currentTimeMillis());
                    return result;
                } catch (HttpStatusCodeException e) {
                    lastCallAt.put(provider, System.currentTimeMillis());
                    int status = e.getStatusCode().value();
                    boolean retryable = status == 429 || status == 503;
                    if (!retryable || attempt > maxRetries) {
                        throw e;
                    }
                    long wait = retryAfterMs(e, backoffMs);
                    log.warn("Appel {} en limite de débit (HTTP {}), tentative {}/{} dans {} ms",
                            provider, status, attempt, maxRetries, wait);
                    sleep(wait);
                    backoffMs *= 2;
                }
            }
        }
    }

    /** Patiente le temps nécessaire pour ne pas dépasser la cadence du fournisseur. */
    private void throttle(String provider, long minInterval) {
        Long last = lastCallAt.get(provider);
        if (last == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed < minInterval) {
            sleep(minInterval - elapsed);
        }
    }

    /** Délai avant retry : en-tête Retry-After (en secondes) s'il est numérique, sinon le back-off. */
    private long retryAfterMs(HttpStatusCodeException e, long fallbackMs) {
        String header = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst("Retry-After") : null;
        if (header != null && !header.isBlank()) {
            try {
                return Math.max(fallbackMs, Long.parseLong(header.trim()) * 1000L);
            } catch (NumberFormatException ignored) {
                // En-tête au format date HTTP : on s'en tient au back-off exponentiel.
            }
        }
        return fallbackMs;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("Appel à l'IA interrompu.");
        }
    }
}
