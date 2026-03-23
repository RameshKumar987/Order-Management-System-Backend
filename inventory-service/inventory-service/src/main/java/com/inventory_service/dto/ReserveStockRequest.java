package com.inventory_service.dto;

public record ReserveStockRequest(
        Long orderId,
        Long productId,
        int quantity
) {}
