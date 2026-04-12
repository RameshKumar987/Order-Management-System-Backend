package com.order_service.dto;

import java.util.List;

public record CreateOrderRequest(
        List<OrderItemRequest> items,
        String deliveryAddress,
        String deliveryCity,
        Double deliveryLat,
        Double deliveryLng
) {}
