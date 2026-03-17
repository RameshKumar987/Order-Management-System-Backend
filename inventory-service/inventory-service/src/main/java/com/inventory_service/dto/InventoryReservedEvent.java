package com.inventory_service.dto;

public record InventoryReservedEvent(

        Long orderId,
        Long productId,
        int quantity

) {}
