package com.payment_service.dto;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        Double amount,
        String status,
        String message
) {}
