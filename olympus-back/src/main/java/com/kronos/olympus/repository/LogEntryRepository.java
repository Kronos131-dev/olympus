package com.kronos.olympus.repository;

import com.kronos.olympus.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    // Les opérations de base (sauvegarde, suppression) seront héritées de JpaRepository.
}
