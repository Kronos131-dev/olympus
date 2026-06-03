package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.MealPlanGenerationRequest;
import com.kronos.olympus.dto.request.MealPlanRequest;
import com.kronos.olympus.dto.request.WeeklyPlanRequest;
import com.kronos.olympus.dto.response.MealPlanResponse;
import com.kronos.olympus.security.UserDetailsImpl;
import com.kronos.olympus.service.mealplan.MealPlanGenerationService;
import com.kronos.olympus.service.mealplan.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanGenerationService mealPlanGenerationService;

    @PostMapping
    public ResponseEntity<MealPlanResponse> createMealPlan(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MealPlanRequest request) {

        MealPlanResponse response = mealPlanService.createMealPlan(userDetails.getUser(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MealPlanResponse>> getUserMealPlans(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return ResponseEntity.ok(mealPlanService.getUserMealPlans(userDetails.getUser().getId()));
    }

    @GetMapping("/weekly")
    public ResponseEntity<MealPlanResponse> getWeeklyPlan(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return ResponseEntity.ok(mealPlanService.getWeeklyPlan(userDetails.getUser()));
    }

    @PutMapping("/weekly")
    public ResponseEntity<MealPlanResponse> saveWeeklyPlan(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WeeklyPlanRequest request) {

        return ResponseEntity.ok(mealPlanService.saveWeeklyPlan(userDetails.getUser(), request));
    }

    @PostMapping("/generate")
    public ResponseEntity<MealPlanResponse> generateWeeklyPlan(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) MealPlanGenerationRequest request) {

        String prompt = request != null ? request.getPrompt() : null;
        return ResponseEntity.ok(
                mealPlanGenerationService.generateWeeklyPlan(userDetails.getUser(), prompt));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealPlanResponse> getMealPlanById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {

        return ResponseEntity.ok(mealPlanService.getMealPlanById(id, userDetails.getUser().getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealPlanResponse> updateMealPlan(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id,
            @Valid @RequestBody MealPlanRequest request) {

        return ResponseEntity.ok(mealPlanService.updateMealPlan(userDetails.getUser(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMealPlan(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {

        mealPlanService.deleteMealPlan(id, userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
