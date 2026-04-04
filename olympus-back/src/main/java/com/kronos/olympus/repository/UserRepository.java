package com.kronos.olympus.repository;

import com.kronos.olympus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Recherche par email pour Spring Security et la vérification de compte existant
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
