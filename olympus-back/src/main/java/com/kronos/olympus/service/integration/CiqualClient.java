package com.kronos.olympus.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CiqualClient {

    // Simulation du client Ciqual
    // En réalité, la base CIQUAL est souvent fournie sous forme de fichier CSV/Excel par l'ANSES
    // L'implémentation standard consiste à importer ce CSV dans une table 'ciqual_data'
    // et à faire des requêtes SQL dessus plutôt que d'appeler une API externe (il n'y a pas d'API REST officielle stable).
    
    public void searchCiqual(String name) {
        log.info("Recherche dans Ciqual (simulée pour l'instant) pour: {}", name);
        // TODO: Implémenter la logique de recherche dans la table Ciqual
    }
}
