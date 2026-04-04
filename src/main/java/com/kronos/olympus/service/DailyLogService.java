package com.kronos.olympus.service;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.exception.EntityNotFoundException;
import com.kronos.olympus.mapper.DailyLogMapper;
import com.kronos.olympus.model.*;
import com.kronos.olympus.repository.DailyLogRepository;
import com.kronos.olympus.repository.FoodItemRepository;
import com.kronos.olympus.repository.LogEntryRepository;
import com.kronos.olympus.repository.MealPresetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final LogEntryRepository logEntryRepository;
    private final FoodItemRepository foodItemRepository;
    private final MealPresetRepository mealPresetRepository;
    private final DailyLogMapper dailyLogMapper;

    @Transactional
    public DailyLogResponse getDailyLogByDate(User user, LocalDate date) {
        DailyLog dailyLog = getOrCreateDailyLog(user, date);
        return dailyLogMapper.toResponse(dailyLog);
    }

    @Transactional
    public DailyLogResponse addLogEntry(User user, LogEntryRequest request) {
        log.info("Ajout d'une entrée au journal pour l'utilisateur {} à la date {}", user.getId(), request.getTargetDate());

        if (request.getFoodItemId() == null && request.getMealPresetId() == null) {
            throw new IllegalArgumentException("Vous devez fournir soit un ID d'aliment, soit un ID de repas pré-enregistré");
        }

        DailyLog dailyLog = getOrCreateDailyLog(user, request.getTargetDate());

        LogEntry logEntry = LogEntry.builder()
                .dailyLog(dailyLog)
                .consumedAt(LocalDateTime.now())
                .build();

        double addedKcal = 0;
        double addedProteins = 0;
        double addedCarbs = 0;
        double addedFats = 0;

        // Cas 1 : L'utilisateur logge un aliment unitaire
        if (request.getFoodItemId() != null) {
            FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Aliment introuvable avec l'ID: " + request.getFoodItemId()));

            if (request.getQuantityGrams() == null || request.getQuantityGrams() <= 0) {
                throw new IllegalArgumentException("La quantité est obligatoire et doit être positive pour un aliment unitaire");
            }

            logEntry.setFoodItem(foodItem);
            logEntry.setQuantityGrams(request.getQuantityGrams());

            double ratio = request.getQuantityGrams() / 100.0;
            addedKcal = foodItem.getKcal100g() * ratio;
            addedProteins = foodItem.getProteins100g() * ratio;
            addedCarbs = foodItem.getCarbs100g() * ratio;
            addedFats = foodItem.getFats100g() * ratio;

        // Cas 2 : L'utilisateur logge un repas entier
        } else if (request.getMealPresetId() != null) {
            MealPreset mealPreset = mealPresetRepository.findById(request.getMealPresetId())
                    .orElseThrow(() -> new EntityNotFoundException("Repas pré-enregistré introuvable avec l'ID: " + request.getMealPresetId()));

            if (!mealPreset.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Vous n'êtes pas autorisé à utiliser ce repas");
            }

            logEntry.setMealPreset(mealPreset);
            logEntry.setQuantityGrams(1.0); // 1 portion du repas

            // On additionne les macros de tous les ingrédients du repas
            for (MealIngredient ingredient : mealPreset.getIngredients()) {
                double ratio = ingredient.getQuantityGrams() / 100.0;
                addedKcal += ingredient.getFoodItem().getKcal100g() * ratio;
                addedProteins += ingredient.getFoodItem().getProteins100g() * ratio;
                addedCarbs += ingredient.getFoodItem().getCarbs100g() * ratio;
                addedFats += ingredient.getFoodItem().getFats100g() * ratio;
            }
        }

        // Mise à jour des totaux du DailyLog
        dailyLog.setTotalKcal(round(dailyLog.getTotalKcal() + addedKcal));
        dailyLog.setTotalProteins(round(dailyLog.getTotalProteins() + addedProteins));
        dailyLog.setTotalCarbs(round(dailyLog.getTotalCarbs() + addedCarbs));
        dailyLog.setTotalFats(round(dailyLog.getTotalFats() + addedFats));

        logEntryRepository.save(logEntry);
        dailyLogRepository.save(dailyLog); // Met à jour les totaux en base

        return dailyLogMapper.toResponse(dailyLog);
    }

    @Transactional
    public DailyLogResponse removeLogEntry(User user, Long entryId) {
        log.info("Suppression de l'entrée ID: {} par l'utilisateur ID: {}", entryId, user.getId());

        LogEntry logEntry = logEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entrée introuvable avec l'ID: " + entryId));

        DailyLog dailyLog = logEntry.getDailyLog();

        if (!dailyLog.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce journal");
        }

        double removedKcal = 0;
        double removedProteins = 0;
        double removedCarbs = 0;
        double removedFats = 0;

        if (logEntry.getFoodItem() != null) {
            double ratio = logEntry.getQuantityGrams() / 100.0;
            removedKcal = logEntry.getFoodItem().getKcal100g() * ratio;
            removedProteins = logEntry.getFoodItem().getProteins100g() * ratio;
            removedCarbs = logEntry.getFoodItem().getCarbs100g() * ratio;
            removedFats = logEntry.getFoodItem().getFats100g() * ratio;
            
        } else if (logEntry.getMealPreset() != null) {
            for (MealIngredient ingredient : logEntry.getMealPreset().getIngredients()) {
                double ratio = ingredient.getQuantityGrams() / 100.0;
                removedKcal += ingredient.getFoodItem().getKcal100g() * ratio;
                removedProteins += ingredient.getFoodItem().getProteins100g() * ratio;
                removedCarbs += ingredient.getFoodItem().getCarbs100g() * ratio;
                removedFats += ingredient.getFoodItem().getFats100g() * ratio;
            }
        }

        // On soustrait et on s'assure de ne pas avoir de valeurs négatives à cause des arrondis
        dailyLog.setTotalKcal(Math.max(0, round(dailyLog.getTotalKcal() - removedKcal)));
        dailyLog.setTotalProteins(Math.max(0, round(dailyLog.getTotalProteins() - removedProteins)));
        dailyLog.setTotalCarbs(Math.max(0, round(dailyLog.getTotalCarbs() - removedCarbs)));
        dailyLog.setTotalFats(Math.max(0, round(dailyLog.getTotalFats() - removedFats)));

        // Suppression de l'entrée et mise à jour du log
        if (dailyLog.getEntries() != null) {
            dailyLog.getEntries().remove(logEntry);
        }
        logEntryRepository.delete(logEntry);
        dailyLogRepository.save(dailyLog);

        return dailyLogMapper.toResponse(dailyLog);
    }

    private DailyLog getOrCreateDailyLog(User user, LocalDate date) {
        return dailyLogRepository.findByUserIdAndTargetDate(user.getId(), date)
                .orElseGet(() -> dailyLogRepository.save(
                        DailyLog.builder()
                                .user(user)
                                .targetDate(date)
                                .totalKcal(0.0)
                                .totalProteins(0.0)
                                .totalCarbs(0.0)
                                .totalFats(0.0)
                                .build()
                ));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
