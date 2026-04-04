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

        if (request.getWeightKg() != null && !request.getWeightKg().equals(user.getCurrentWeightKg())) {
            user.setCurrentWeightKg(request.getWeightKg());
            weightChanged = true;
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

        User updatedUser = userRepository.save(user);

        // Si le poids, l'activité ou l'objectif a changé, on recalcule les calories cibles
        // et on enregistre un nouveau point de donnée dans l'historique UserMetrics
        if (needsNewMetrics) {
            recordNewMetrics(updatedUser, weightChanged);
        }

        return userMapper.toResponse(updatedUser);
    }

    private void recordNewMetrics(User user, boolean weightChanged) {
        Integer newCalorieGoal = calculateCalorieGoal(user);
        
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

    private Integer calculateCalorieGoal(User user) {
        double bmr;
        // Formule de Mifflin-St Jeor
        if (user.getGender() == com.kronos.olympus.model.enums.Gender.MALE) {
            bmr = 10 * user.getCurrentWeightKg() + 6.25 * user.getHeightCm() - 5 * 25 + 5; // On hardcode l'âge à 25 pour l'exemple comme dans AuthService
        } else {
            bmr = 10 * user.getCurrentWeightKg() + 6.25 * user.getHeightCm() - 5 * 25 - 161;
        }

        double activityMultiplier = switch (user.getActivityLevel()) {
            case SEDENTARY -> 1.2;
            case LIGHT -> 1.375;
            case MODERATE -> 1.55;
            case INTENSE -> 1.725;
        };

        double maintenanceCalories = bmr * activityMultiplier;

        double targetCalories = switch (user.getGoal()) {
            case LOSE_WEIGHT -> maintenanceCalories - 500;
            case MAINTAIN -> maintenanceCalories;
            case GAIN_MUSCLE -> maintenanceCalories + 300;
        };

        return (int) targetCalories;
    }
}
