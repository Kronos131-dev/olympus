package com.kronos.olympus.mapper;

import com.kronos.olympus.dto.request.RegisterRequest;
import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public abstract class UserMapper {

    // Mapper pour convertir l'entité en réponse
    public abstract UserResponse toResponse(User user);

    // Mapper pour convertir la requête d'inscription en entité
    @Mapping(target = "currentWeightKg", source = "weightKg")
    public abstract User toEntity(RegisterRequest request);

    @AfterMapping
    protected void calculateMacros(User user, @MappingTarget UserResponse response) {
        
        // --- Calcul de la base automatique ---
        if (user.getCurrentWeightKg() != null && user.getHeightCm() != null && 
            user.getGender() != null && user.getActivityLevel() != null && user.getGoal() != null) {
            
            // Calcul de l'âge à partir de la date de naissance
            int age = 25; // default fallback
            if (user.getBirthDate() != null) {
                age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
            }
            
            // 1. Calcul du métabolisme de base (BMR) - Formule de Mifflin-St Jeor
            double bmr = (10.0 * user.getCurrentWeightKg()) + (6.25 * user.getHeightCm()) - (5.0 * age);
            bmr += (user.getGender() == com.kronos.olympus.model.enums.Gender.MALE) ? 5.0 : -161.0;

            // 2. Dépense énergétique journalière (TDEE)
            double multiplier = switch (user.getActivityLevel()) {
                case SEDENTARY -> 1.2;
                case LIGHT -> 1.375;
                case MODERATE -> 1.55;
                case INTENSE -> 1.725;
            };
            double tdee = bmr * multiplier;

            // 3. Calories cibles ajustées selon l'objectif
            double calorieModifier = switch (user.getGoal()) {
                case LOSE_WEIGHT -> 0.85;
                case MAINTAIN -> 1.0;
                case GAIN_MUSCLE -> 1.10;
            };
            double targetCalories = tdee * calorieModifier;

            // 4. Répartition des macronutriments
            double proteinPerKg = switch (user.getGoal()) {
                case LOSE_WEIGHT -> 2.0;
                case MAINTAIN -> 1.6;
                case GAIN_MUSCLE -> 1.8;
            };
            double proteins = user.getCurrentWeightKg() * proteinPerKg;
            
            // Lipides fixés à 25% de l'apport calorique total cible (1g de lipide = 9 kcal)
            double fats = (targetCalories * 0.25) / 9.0;
            
            // Glucides : on alloue le reste des calories disponibles (1g prot = 4kcal, 1g glucide = 4kcal)
            double carbs = (targetCalories - (proteins * 4.0) - (fats * 9.0)) / 4.0;
            if (carbs < 0) carbs = 0; 
            
            // On set les valeurs par defaut calculées par l'algo
            response.setTargetKcal((double) Math.round(targetCalories));
            response.setTargetProteins((double) Math.round(proteins));
            response.setTargetFats((double) Math.round(fats));
            response.setTargetCarbs((double) Math.round(carbs));
        }

        // Si l'utilisateur a désactivé le calcul automatique, on écrase avec ses quotas manuels
        if (Boolean.FALSE.equals(user.getAutoCalculateTargets())) {
            if (user.getManualTargetKcal() != null) response.setTargetKcal(user.getManualTargetKcal());
            if (user.getManualTargetProteins() != null) response.setTargetProteins(user.getManualTargetProteins());
            if (user.getManualTargetCarbs() != null) response.setTargetCarbs(user.getManualTargetCarbs());
            if (user.getManualTargetFats() != null) response.setTargetFats(user.getManualTargetFats());
        }
    }
}