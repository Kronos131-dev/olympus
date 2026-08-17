package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.AiMealRequest;
import com.kronos.olympus.dto.request.MealConfirmationRequest;
import com.kronos.olympus.dto.request.MealCorrectionRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.dto.response.MealAnalysisResponse;
import com.kronos.olympus.security.UserDetailsImpl;
import com.kronos.olympus.service.ai.MealAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/meal-analysis")
@RequiredArgsConstructor
public class MealAnalysisController {
    private final MealAnalysisService mealAnalysisService;

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MealAnalysisResponse> analyzePhoto(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "note", required = false) String note) {
        return ResponseEntity.ok(mealAnalysisService.analyzePhoto(userDetails.getUser(), image, note));
    }

    @PostMapping("/text")
    public ResponseEntity<MealAnalysisResponse> analyzeText(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AiMealRequest request) {
        return ResponseEntity.ok(
                mealAnalysisService.analyzeText(userDetails.getUser(), request.getDescription()));
    }

    @PostMapping("/correct")
    public ResponseEntity<MealAnalysisResponse> correct(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MealCorrectionRequest request) {
        return ResponseEntity.ok(mealAnalysisService.correct(userDetails.getUser(), request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<DailyLogResponse> confirm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MealConfirmationRequest request) {
        return ResponseEntity.ok(mealAnalysisService.confirm(userDetails.getUser(), request));
    }
}
