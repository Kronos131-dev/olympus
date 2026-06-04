package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.UpdateProfileRequest;
import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.mapper.UserMapper;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.UserMetrics;
import com.kronos.olympus.repository.UserMetricsRepository;
import com.kronos.olympus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMetricsRepository userMetricsRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Mise à jour du profil pour l'utilisateur {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        boolean weightChanged = false;
        boolean needsNewMetrics = false;

        if (request.getCurrentWeightKg() != null && !request.getCurrentWeightKg().equals(user.getCurrentWeightKg())) {
            user.setCurrentWeightKg(request.getCurrentWeightKg());
            weightChanged = true;
            needsNewMetrics = true;
        }

        if (request.getHeightCm() != null && !request.getHeightCm().equals(user.getHeightCm())) {
            user.setHeightCm(request.getHeightCm());
            needsNewMetrics = true;
        }

        if (request.getGender() != null && !request.getGender().equals(user.getGender())) {
            user.setGender(request.getGender());
            needsNewMetrics = true;
        }

        if (request.getBirthDate() != null && !request.getBirthDate().equals(user.getBirthDate())) {
            user.setBirthDate(request.getBirthDate());
            needsNewMetrics = true;
        }

        if (request.getActivityLevel() != null && !request.getActivityLevel().equals(user.getActivityLevel())) {
            user.setActivityLevel(request.getActivityLevel());
            needsNewMetrics = true;
        }

        if (request.getGoal() != null && !request.getGoal().equals(user.getGoal())) {
            user.setGoal(request.getGoal());
            needsNewMetrics = true;
        }
        
        if (request.getAiProvider() != null) {
            user.setAiProvider(request.getAiProvider());
        }

        if (request.getAutoCalculateTargets() != null) {
            user.setAutoCalculateTargets(request.getAutoCalculateTargets());
            
            if (!request.getAutoCalculateTargets()) {
                if (request.getManualTargetKcal() != null) user.setManualTargetKcal(request.getManualTargetKcal());
                if (request.getManualTargetProteins() != null) user.setManualTargetProteins(request.getManualTargetProteins());
                if (request.getManualTargetCarbs() != null) user.setManualTargetCarbs(request.getManualTargetCarbs());
                if (request.getManualTargetFats() != null) user.setManualTargetFats(request.getManualTargetFats());
            }
            needsNewMetrics = true;
        }

        User updatedUser = userRepository.save(user);

        if (needsNewMetrics) {
            recordNewMetrics(updatedUser, weightChanged);
        }

        return userMapper.toResponse(updatedUser);
    }

    /**
     * Met à jour le poids d'un utilisateur identifié par son e-mail, puis enregistre la
     * métrique du jour. Utilisé par la synchronisation depuis une app tierce liée (Chiron).
     */
    @Transactional
    public void updateWeightByEmail(String email, Double weightKg) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (weightKg == null || weightKg.equals(user.getCurrentWeightKg())) {
            return; // Rien à faire : poids identique ou absent.
        }

        user.setCurrentWeightKg(weightKg);
        User updatedUser = userRepository.save(user);
        recordNewMetrics(updatedUser, true);
        log.info("Poids synchronisé depuis une app tierce pour {} : {} kg", email, weightKg);
    }

    private void recordNewMetrics(User user, boolean weightChanged) {
        // Utilisera les mêmes cibles que celles envoyées dans UserResponse par le mapper
        UserResponse response = userMapper.toResponse(user);
        Integer newCalorieGoal = response.getTargetKcal() != null ? response.getTargetKcal().intValue() : 2000;
        
        // On vérifie s'il existe déjà une métrique pour aujourd'hui
        LocalDate today = LocalDate.now();
        Optional<UserMetrics> existingMetricToday = userMetricsRepository.findTopByUserIdOrderByRecordedDateDesc(user.getId())
                .filter(m -> m.getRecordedDate().equals(today));

        if (existingMetricToday.isPresent()) {
            // Si oui, on met simplement à jour celle d'aujourd'hui
            UserMetrics metric = existingMetricToday.get();
            metric.setWeightKg(user.getCurrentWeightKg());
            metric.setCalorieGoal(newCalorieGoal);
            userMetricsRepository.save(metric);
            log.info("Mise à jour de la métrique existante pour aujourd'hui: Poids {} kg, Objectif {} kcal", user.getCurrentWeightKg(), newCalorieGoal);
        } else {
            // Sinon on crée une nouvelle entrée dans l'historique
            UserMetrics newMetric = UserMetrics.builder()
                    .user(user)
                    .weightKg(user.getCurrentWeightKg())
                    .calorieGoal(newCalorieGoal)
                    .recordedDate(today)
                    .build();
            userMetricsRepository.save(newMetric);
            log.info("Nouvelle métrique enregistrée dans l'historique: Poids {} kg, Objectif {} kcal", user.getCurrentWeightKg(), newCalorieGoal);
        }
    }
}