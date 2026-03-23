package com.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product ID required")
        Long productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}
