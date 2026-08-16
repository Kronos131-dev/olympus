package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.LogEntryRequest;
import com.kronos.olympus.dto.request.UpdateActivityRequest;
import com.kronos.olympus.dto.request.UpdateLogEntryRequest;
import com.kronos.olympus.dto.response.DailyLogResponse;
import com.kronos.olympus.security.UserDetailsImpl;
import com.kronos.olympus.service.DailyLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/daily-logs")
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService dailyLogService;

    // Récupérer le journal d'une journée précise
    @GetMapping("/{date}")
    public ResponseEntity<DailyLogResponse> getDailyLogByDate(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        DailyLogResponse response = dailyLogService.getDailyLogByDate(userDetails.getUser(), date);
        return ResponseEntity.ok(response);
    }

    // Ajouter une entrée (Aliment ou Repas) à une journée
    @PostMapping("/entries")
    public ResponseEntity<DailyLogResponse> addLogEntry(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody LogEntryRequest request) {
        
        DailyLogResponse response = dailyLogService.addLogEntry(userDetails.getUser(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<DailyLogResponse> updateLogEntry(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateLogEntryRequest request) {

        DailyLogResponse response = dailyLogService.updateLogEntry(userDetails.getUser(), entryId, request);
        return ResponseEntity.ok(response);
    }

    // Supprimer une entrée du journal
    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<DailyLogResponse> removeLogEntry(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long entryId) {
        
        DailyLogResponse response = dailyLogService.removeLogEntry(userDetails.getUser(), entryId);
        return ResponseEntity.ok(response);
    }

    // Mettre à jour l'activité physique du jour (Pas et Entraînement)
    @PutMapping("/activity")
    public ResponseEntity<DailyLogResponse> updateActivity(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateActivityRequest request) {
        
        DailyLogResponse response = dailyLogService.updateActivity(userDetails.getUser(), request);
        return ResponseEntity.ok(response);
    }
}
