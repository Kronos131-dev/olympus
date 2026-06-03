package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Liaison permanente entre un compte Olympus et une application tierce de confiance
 * (ex. Chiron). Le {@code token} est une chaîne opaque, sans expiration : une fois
 * créé, le lien reste valide indéfiniment tant qu'il n'est pas explicitement révoqué.
 */
@Entity
@Table(name = "integration_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chaîne opaque aléatoire (pas un JWT) : sert de clé d'accès permanente.
    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Application tierce cliente (ex. "CHIRON"). */
    @Column(nullable = false, length = 40)
    private String client;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.revoked == null) {
            this.revoked = false;
        }
    }
}
