package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.AuthRequest;
import com.kronos.olympus.dto.request.RegisterRequest;
import com.kronos.olympus.dto.response.AuthResponse;
import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.mapper.UserMapper;
import com.kronos.olympus.model.RefreshToken;
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
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ce pseudo est déjà utilisé");
        }

        // Création de l'utilisateur
        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER); // Par défaut

        User savedUser = userRepository.save(user);

        // On utilise la méthode du UserMapper pour avoir le calcul précis du Kcal cible
        UserResponse responseDto = userMapper.toResponse(savedUser);
        int initialCalorieGoal = responseDto.getTargetKcal() != null ? responseDto.getTargetKcal().intValue() : 2500;

        // Initialisation de la première métrique de poids
        UserMetrics initialMetrics = UserMetrics.builder()
                .user(savedUser)
                .weightKg(request.getWeightKg())
                .calorieGoal(initialCalorieGoal)
                .recordedDate(LocalDate.now())
                .build();
        userMetricsRepository.save(initialMetrics);

        return buildAuthResponse(savedUser, responseDto);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable après authentification"));

        return buildAuthResponse(user, userMapper.toResponse(user));
    }

    /**
     * Échange un refresh token valide contre un nouveau couple access/refresh (rotation).
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken rotated = refreshTokenService.verifyAndRotate(refreshToken);
        User user = rotated.getUser();

        UserDetails userDetails = new UserDetailsImpl(user);
        String accessToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(rotated.getToken())
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }

    /**
     * Révoque le refresh token (et toute sa famille) lors de la déconnexion. Idempotent.
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeByToken(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, UserResponse responseDto) {
        UserDetails userDetails = new UserDetailsImpl(user);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .user(responseDto)
                .build();
    }
}
