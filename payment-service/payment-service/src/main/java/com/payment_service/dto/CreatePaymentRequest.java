package com.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(

        @NotNull(message = "OrderId is required")
        Long orderId,

        @NotNull(message = "UserId is required")
        Long userId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Double amount,

        @NotBlank(message = "Payment method required")
        String method
) {}
