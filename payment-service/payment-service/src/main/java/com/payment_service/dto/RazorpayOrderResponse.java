package com.payment_service.dto;

import lombok.Builder;

@Builder
public record RazorpayOrderResponse(
        String id,
        String rzpOrderId,
        Double amount,
        String currency,
        String keyId,
        Long orderId
) {
}
