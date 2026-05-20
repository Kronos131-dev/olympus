package com.kronos.olympusfront.network.dto;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private UserResponse user;

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}
