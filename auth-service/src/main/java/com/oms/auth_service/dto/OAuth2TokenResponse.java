package com.oms.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuth2TokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        Long expiresIn,

        String role,
        String scope
) {
    public OAuth2TokenResponse(String accessToken, String refreshToken, Long expiresIn, String role, String scope) {
        this(accessToken, refreshToken, "Bearer", expiresIn, role, scope);
    }
}

