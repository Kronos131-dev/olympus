package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.response.DailyMicronutrientsResponse;
import com.kronos.olympus.dto.response.MicronutrientResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.model.enums.Role;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class MicronutrientServiceTest {
    @Autowired
    private MicronutrientService micronutrientService;
    @Autowired
    private DailyLogService dailyLogService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;

    private User user;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("micros-" + UUID.randomUUID() + "@olympus.test")
                .passwordHash("hash")
                .role(Role.USER)
                .gender(Gender.MALE)
                .heightCm(180.0)
                .currentWeightKg(80.0)
                .birthDate(LocalDate.now().minusYears(30))
                .activityLevel(ActivityLevel.MODERATE)
                .goal(Goal.MAINTAIN)
                .build());
    }

    @Test
    void getDailyMicronutrients_foodWithKnownMicros_reportsFullCoverage() {
        logFood(knownFood(50.0), 200);

        MicronutrientResponse magnesium = magnesiumOf(
                micronutrientService.getDailyMicronutrients(user, today));

        assertThat(magnesium.getConsumed()).isEqualTo(100.0);
        assertThat(magnesium.getCoverage()).isEqualTo(1.0);
        assertThat(magnesium.getReference()).isEqualTo(380.0);
        assertThat(magnesium.getUnit()).isEqualTo("mg");
    }

    @Test
    void getDailyMicronutrients_foodWithoutMicros_lowersCoverageWithoutSkewingTotal() {
        logFood(knownFood(50.0), 100);
        logFood(foodWithoutMicros(), 100);

        MicronutrientResponse magnesium = magnesiumOf(
                micronutrientService.getDailyMicronutrients(user, today));

        assertThat(magnesium.getConsumed()).isEqualTo(50.0);
        assertThat(magnesium.getCoverage()).isEqualTo(0.5);
    }

    @Test
    void getDailyMicronutrients_emptyDay_returnsEveryNutrientAtZero() {
        DailyMicronutrientsResponse response = micronutrientService.getDailyMicronutrients(user, today);

        assertThat(response.getNutrients()).hasSize(Nutrient.values().length);
        assertThat(response.getNutrients()).allMatch(n -> n.getConsumed() == 0.0);
        assertThat(response.getOverallCoverage()).isZero();
    }

    @Test
    void getDailyMicronutrients_female_usesTheFemaleReference() {
        user.setGender(Gender.FEMALE);
        userRepository.save(user);

        MicronutrientResponse iron = micronutrientService.getDailyMicronutrients(user, today)
                .getNutrients().stream()
                .filter(n -> n.getNutrient() == Nutrient.IRON)
                .findFirst()
                .orElseThrow();

        assertThat(iron.getReference()).isEqualTo(16.0);
    }

    private FoodItem knownFood(double magnesiumPer100g) {
        return foodItemRepository.save(FoodItem.builder()
                .name("Aliment documenté")
                .kcal100g(100.0).proteins100g(0.0).carbs100g(0.0).fats100g(0.0)
                .micros100g(Map.of(Nutrient.MAGNESIUM, magnesiumPer100g))
                .source(FoodSource.CIQUAL)
                .build());
    }

    private FoodItem foodWithoutMicros() {
        return foodItemRepository.save(FoodItem.builder()
                .name("Produit scanné sans micros")
                .kcal100g(100.0).proteins100g(0.0).carbs100g(0.0).fats100g(0.0)
                .source(FoodSource.OFF)
                .build());
    }

    private void logFood(FoodItem food, double grams) {
        dailyLogService.addLogEntry(user, LogEntryRequest.builder()
                .targetDate(today)
                .foodItemId(food.getId())
                .quantityGrams(grams)
                .build());
    }

    private MicronutrientResponse magnesiumOf(DailyMicronutrientsResponse response) {
        return response.getNutrients().stream()
                .filter(n -> n.getNutrient() == Nutrient.MAGNESIUM)
                .findFirst()
                .orElseThrow();
    }
}
