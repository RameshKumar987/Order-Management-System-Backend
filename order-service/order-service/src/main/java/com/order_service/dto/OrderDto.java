package com.order_service.dto;

import java.util.List;

public record OrderDto(
        Long id,
        Long userId,
        String status,
        List<OrderItemDto> items
) {
}