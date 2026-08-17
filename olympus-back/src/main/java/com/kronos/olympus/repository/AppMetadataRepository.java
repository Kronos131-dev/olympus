package com.kronos.olympus.repository;

import com.kronos.olympus.model.AppMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppMetadataRepository extends JpaRepository<AppMetadata, String> {
}
