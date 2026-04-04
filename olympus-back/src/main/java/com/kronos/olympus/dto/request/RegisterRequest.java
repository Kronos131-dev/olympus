package com.kronos.olympus.dto.request;

import com.kronos.olympus.model.enums.ActivityLevel;
import com.kronos.olympus.model.enums.Gender;
import com.kronos.olympus.model.enums.Goal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotNull(message = "Le genre est obligatoire")
    private Gender gender;

    @NotNull(message = "La taille est obligatoire")
    @Positive(message = "La taille doit être positive")
    private Double heightCm;

    @NotNull(message = "Le poids est obligatoire")
    @Positive(message = "Le poids doit être positif")
    private Double weightKg;

    @NotNull(message = "Le niveau d'activité est obligatoire")
    private ActivityLevel activityLevel;

    @NotNull(message = "L'objectif est obligatoire")
    private Goal goal;
}
