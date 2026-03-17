package com.oms.auth_service.dto;

public record AuthResponse(
        String token,
        String role,
        String refreshToken,
        Long accessTokenExpiresIn
) {
    public AuthResponse(String token, String role, String refreshToken) {
        this(token, role, refreshToken, 900L); // 15 minutes default
    }
}

