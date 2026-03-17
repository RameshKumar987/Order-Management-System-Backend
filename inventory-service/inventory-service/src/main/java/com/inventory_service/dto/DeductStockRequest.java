package com.inventory_service.dto;

/**
 * DTO for deducting stock directly (without reservation)
 */
public record DeductStockRequest(
        Long productId,
        int quantity,
        String reason
) {}

