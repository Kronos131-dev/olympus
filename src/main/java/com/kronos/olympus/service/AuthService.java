package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.AuthRequest;
import com.kronos.olympus.dto.request.RegisterRequest;
import com.kronos.olympus.dto.response.AuthResponse;
import com.kronos.olympus.mapper.UserMapper;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.UserMetrics;
import com.kronos.olympus.model.enums.Role;
import com.kronos.olympus.repository.UserMetricsRepository;
import com.kronos.olympus.repository.UserRepository;
import com.kronos.olympus.security.JwtService;
import com.kronos.olympus.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMetricsRepository userMetricsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Création de l'utilisateur
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER); // Par défaut

        User savedUser = userRepository.save(user);

        // Initialisation de la première métrique de poids
        UserMetrics initialMetrics = UserMetrics.builder()
                .user(savedUser)
                .weightKg(request.getWeightKg())
                .calorieGoal(calculateInitialCalorieGoal(request)) // Calcul très basique pour l'exemple
                .recordedDate(LocalDate.now())
                .build();
        userMetricsRepository.save(initialMetrics);

        UserDetails userDetails = new UserDetailsImpl(savedUser);
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable après authentification"));

        UserDetails userDetails = new UserDetailsImpl(user);
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .user(userMapper.toResponse(user))
                .build();
    }

    // Méthode simplifiée pour calculer les calories cibles selon l'objectif et le niveau d'activité
    private Integer calculateInitialCalorieGoal(RegisterRequest request) {
        double bmr;
        // Formule de Mifflin-St Jeor
        if (request.getGender() == com.kronos.olympus.model.enums.Gender.MALE) {
            bmr = 10 * request.getWeightKg() + 6.25 * request.getHeightCm() - 5 * 25 + 5; // 25 = âge moyen pris pour l'exemple
        } else {
            bmr = 10 * request.getWeightKg() + 6.25 * request.getHeightCm() - 5 * 25 - 161;
        }

        double activityMultiplier = switch (request.getActivityLevel()) {
            case SEDENTARY -> 1.2;
            case LIGHT -> 1.375;
            case MODERATE -> 1.55;
            case INTENSE -> 1.725;
        };

        double maintenanceCalories = bmr * activityMultiplier;

        double targetCalories = switch (request.getGoal()) {
            case LOSE_WEIGHT -> maintenanceCalories - 500;
            case MAINTAIN -> maintenanceCalories;
            case GAIN_MUSCLE -> maintenanceCalories + 300;
        };

        return (int) targetCalories;
    }
}
