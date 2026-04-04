package com.order_service.dto;

public record OrderItemDto(
        Long productId,
        String productName,
        Integer quantity,
        Double price
) {
}