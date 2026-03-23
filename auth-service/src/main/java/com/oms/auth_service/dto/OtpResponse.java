package com.oms.auth_service.dto;

public record OtpResponse(
        Boolean success,
        String message,
        Long expiryTime
) {}

