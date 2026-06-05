package com.kronos.olympus.repository;

import com.kronos.olympus.model.PasswordResetToken;
import com.kronos.olympus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // Invalide les demandes précédentes non consommées avant d'en émettre une nouvelle.
    void deleteByUserAndUsedFalse(User user);
}
