package com.kronos.olympus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    // Access token JWT court (durée jwtExpiration)
    private String token;
    // Refresh token opaque longue durée
    private String refreshToken;
    // Durée de vie de l'access token en secondes (indication pour le client)
    private Long expiresIn;
    private UserResponse user;
}
