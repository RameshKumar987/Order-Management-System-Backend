package com.inventory_service.dto;

/**
 * DTO for releasing reserved stock back to available stock
 */
public record ReleaseStockRequest(
        Long orderId,
        Long productId,
        int quantity
) {}

