package com.kronos.olympus.dto.response;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private Gender gender;
    private Double heightCm;
    private Double currentWeightKg;
    private ActivityLevel activityLevel;
    private Goal goal;
    private LocalDateTime createdAt;
}
