package com.kronos.olympus.repository;

import com.kronos.olympus.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    // Recherche scopée à l'utilisateur : garantit qu'on ne lit pas la conversation d'autrui
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    Optional<Conversation> findTopByUserIdOrderByUpdatedAtDesc(Long userId);
}
