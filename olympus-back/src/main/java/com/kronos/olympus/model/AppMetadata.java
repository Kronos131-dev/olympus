package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Petit magasin clé/valeur pour l'état interne de l'application — aujourd'hui la version du
 * référentiel CIQUAL déjà chargée, qui évite de rejouer plusieurs milliers de lignes à chaque
 * démarrage. Entité plutôt que table Flyway : le profil de test désactive Flyway et construit
 * tout le schéma depuis les entités.
 */
@Entity
@Table(name = "app_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppMetadata {

    @Id
    @Column(name = "key", length = 64)
    private String key;

    @Column(name = "value", nullable = false)
    private String value;
}
