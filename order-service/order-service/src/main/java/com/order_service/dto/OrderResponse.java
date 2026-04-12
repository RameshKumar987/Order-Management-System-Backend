package com.order_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String status,
        List<OrderItemDto> items,
        Double totalAmount,
        LocalDateTime createdAt,
        String deliveryAddress,
        String deliveryCity,
        Double deliveryLat,
        Double deliveryLng
) {}


