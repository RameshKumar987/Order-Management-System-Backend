package com.inventory_service.dto;

import java.util.List;

public record OrderDto(
        Long id,
        Long userId,
        String status,
        List<OrderItemDto> items,
        String deliveryAddress,
        String deliveryCity,
        Double deliveryLat,
        Double deliveryLng
) {
}