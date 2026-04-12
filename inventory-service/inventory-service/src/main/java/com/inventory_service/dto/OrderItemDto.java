package com.inventory_service.dto;

public record OrderItemDto(
        Long productId,
        Integer quantity,
        Double price
) {
}