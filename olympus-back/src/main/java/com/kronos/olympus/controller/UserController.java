package com.kronos.olympus.controller;

import com.kronos.olympus.dto.request.UpdateProfileRequest;
import com.kronos.olympus.dto.response.UserResponse;
import com.kronos.olympus.security.UserDetailsImpl;
import com.kronos.olympus.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserResponse response = userService.getUserProfile(userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UserResponse response = userService.updateProfile(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(response);
    }
}
