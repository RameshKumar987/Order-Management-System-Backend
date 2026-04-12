package com.payment_service.dto;

import lombok.Builder;

@Builder
public record ApiResponse(
        boolean success,
        String message,
        Object data
) {}
