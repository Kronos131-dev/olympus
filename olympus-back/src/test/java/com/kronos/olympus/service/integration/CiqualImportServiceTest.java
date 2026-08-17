package com.kronos.olympus.service.integration;

import com.kronos.olympus.model.AppMetadata;
import com.kronos.olympus.model.FoodItem;
import com.kronos.olympus.model.enums.FoodSource;
import com.kronos.olympus.model.enums.Nutrient;
import com.kronos.olympus.repository.AppMetadataRepository;
import com.kronos.olympus.repository.FoodItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CiqualImportServiceTest {
    @Autowired
    private CiqualImportService ciqualImportService;
    @Autowired
    private FoodItemRepository foodItemRepository;
    @Autowired
    private AppMetadataRepository appMetadataRepository;

    @Test
    void run_sameVersion_doesNotReimport() {
        long before = foodItemRepository.findByCiqualCodeIsNotNull().size();

        ciqualImportService.run();

        assertThat(foodItemRepository.findByCiqualCodeIsNotNull()).hasSize((int) before);
    }

    @Test
    void run_newVersion_updatesInPlaceWithoutDuplicating() {
        List<FoodItem> initial = foodItemRepository.findByCiqualCodeIsNotNull();
        assertThat(initial).isNotEmpty();

        FoodItem sample = initial.get(0);
        Long sampleId = sample.getId();
        Integer sampleCode = sample.getCiqualCode();
        sample.setKcal100g(-1.0);
        foodItemRepository.save(sample);

        String appliedVersion = (String) ReflectionTestUtils.getField(ciqualImportService, "csvVersion");
        try {
            ReflectionTestUtils.setField(ciqualImportService, "csvVersion", "test-rerun");
            ciqualImportService.run();

            List<FoodItem> after = foodItemRepository.findByCiqualCodeIsNotNull();
            assertThat(after).hasSameSizeAs(initial);

            FoodItem reimported = after.stream()
                    .filter(item -> sampleCode.equals(item.getCiqualCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(reimported.getId()).isEqualTo(sampleId);
            assertThat(reimported.getKcal100g()).isNotEqualTo(-1.0);
        } finally {
            ReflectionTestUtils.setField(ciqualImportService, "csvVersion", appliedVersion);
            appMetadataRepository.save(new AppMetadata("ciqual.version", appliedVersion));
        }
    }

    @Test
    @Transactional
    void run_unknownNutrientValue_staysNullRatherThanZero() {
        List<FoodItem> foods = foodItemRepository.findByCiqualCodeIsNotNull();

        long complete = foods.stream()
                .filter(food -> food.getMicros100g().size() == Nutrient.values().length)
                .count();
        assertThat(complete).isLessThan(foods.size());
        assertThat(foods).allMatch(food -> food.getSource() == FoodSource.CIQUAL);
    }
}
