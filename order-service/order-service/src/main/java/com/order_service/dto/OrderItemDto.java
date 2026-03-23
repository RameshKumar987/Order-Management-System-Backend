package com.order_service.dto;

public record OrderItemDto(
        Long productId,
        Integer quantity,
        Double price
) {
}