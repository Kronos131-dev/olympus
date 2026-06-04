package com.kronos.olympus.service;

import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.mapper.MealPresetMapper;
import com.kronos.olympus.model.MealPreset;
import com.kronos.olympus.model.User;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.LogEntryRepository;
import com.kronos.olympus.repository.MealIngredientRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import com.kronos.olympus.repository.PlannedMealEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie la suppression robuste d'un repas pré-enregistré (Fix 1) : avant de supprimer le
 * preset, on détache l'historique (LogEntry) et on purge le planning (PlannedMealEntry) pour
 * éviter la violation de contrainte FK qui provoquait un 500 quand le repas était utilisé.
 */
@ExtendWith(MockitoExtension.class)
class MealPresetServiceTest {

    @Mock
    private MealPresetRepository mealPresetRepository;
    @Mock
    private FoodItemRepository foodItemRepository;
    @Mock
    private MealIngredientRepository mealIngredientRepository;
    @Mock
    private LogEntryRepository logEntryRepository;
    @Mock
    private PlannedMealEntryRepository plannedMealEntryRepository;
    @Mock
    private MealPresetMapper mealPresetMapper;
    @Mock
    private MealPresetTotalsCalculator totalsCalculator;
    @InjectMocks
    private MealPresetService mealPresetService;

    @Test
    void deleteMealPreset_detacheHistoriqueEtPurgePlanningAvantSuppression() {
        MealPreset preset = MealPreset.builder()
                .id(5L)
                .user(User.builder().id(1L).build())
                .name("Bol Olympien")
                .build();
        when(mealPresetRepository.findById(5L)).thenReturn(Optional.of(preset));

        mealPresetService.deleteMealPreset(5L, 1L);

        // L'ordre est essentiel : on doit lever les références AVANT le delete.
        InOrder order = inOrder(logEntryRepository, plannedMealEntryRepository,
                mealIngredientRepository, mealPresetRepository);
        order.verify(logEntryRepository).detachMealPreset(5L);
        order.verify(plannedMealEntryRepository).deleteByMealPresetId(5L);
        order.verify(mealIngredientRepository).deleteByMealPresetId(5L);
        order.verify(mealPresetRepository).delete(preset);
    }

    @Test
    void deleteMealPreset_refuseLaSuppressionParUnAutreUtilisateur() {
        MealPreset preset = MealPreset.builder()
                .id(5L)
                .user(User.builder().id(2L).build())
                .build();
        when(mealPresetRepository.findById(5L)).thenReturn(Optional.of(preset));

        assertThrows(IllegalArgumentException.class,
                () -> mealPresetService.deleteMealPreset(5L, 1L));

        verify(logEntryRepository, never()).detachMealPreset(org.mockito.ArgumentMatchers.anyLong());
        verify(plannedMealEntryRepository, never()).deleteByMealPresetId(org.mockito.ArgumentMatchers.anyLong());
        verify(mealPresetRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteMealPreset_lanceUneErreurSiIntrouvable() {
        when(mealPresetRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> mealPresetService.deleteMealPreset(5L, 1L));
    }
}
