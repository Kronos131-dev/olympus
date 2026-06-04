package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.User;
import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que l'ajout d'une entrée au journal renvoie une réponse qui CONTIENT la nouvelle
 * entrée (et pas seulement les totaux). Régression : addLogEntry sauvegardait la LogEntry mais
 * ne l'ajoutait pas à dailyLog.getEntries(), si bien que l'entrée n'apparaissait pas dans
 * l'historique tant qu'un refetch n'avait pas eu lieu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class DailyLogServiceIntegrationTest {

    @Autowired
    private DailyLogService dailyLogService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("dailylog-test-" + System.nanoTime() + "@olympus.test")
                .passwordHash("x")
                .role(Role.USER)
                .gender(Gender.MALE)
                .heightCm(180.0)
                .currentWeightKg(75.0)
                .birthDate(LocalDate.of(1995, 1, 1))
                .activityLevel(ActivityLevel.MODERATE)
                .goal(Goal.MAINTAIN)
                .build());
    }

    @Test
    void addLogEntry_returnsResponseContainingTheNewEntry() {
        FoodItem food = foodItemRepository.saveAndFlush(FoodItem.builder()
                .name("Zorpavoine")
                .barcode("CIQUAL-Zorpavoine-" + System.nanoTime())
                .kcal100g(100.0).proteins100g(10.0).carbs100g(20.0).fats100g(5.0)
                .source(FoodSource.CIQUAL)
                .build());

        LogEntryRequest request = LogEntryRequest.builder()
                .targetDate(LocalDate.now())
                .foodItemId(food.getId())
                .quantityGrams(200.0)
                .build();

        DailyLogResponse response = dailyLogService.addLogEntry(user, request);

        assertTrue(response.getTotalKcal() > 0, "Les totaux doivent être mis à jour");
        assertEquals(1, response.getEntries().size(),
                "La réponse doit contenir la nouvelle entrée");
        assertEquals(food.getId(), response.getEntries().get(0).getFoodItem().getId());
    }
}
