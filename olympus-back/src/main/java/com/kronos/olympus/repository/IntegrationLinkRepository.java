package com.kronos.olympus.repository;

import com.kronos.olympus.model.IntegrationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntegrationLinkRepository extends JpaRepository<IntegrationLink, Long> {

    Optional<IntegrationLink> findByTokenAndRevokedFalse(String token);

    Optional<IntegrationLink> findFirstByUserIdAndClientAndRevokedFalse(Long userId, String client);
}
