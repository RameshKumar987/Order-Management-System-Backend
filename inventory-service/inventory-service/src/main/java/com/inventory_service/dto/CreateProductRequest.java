package com.inventory_service.dto;

public record CreateProductRequest(

        String name,
        String description,
        double price,
        Long categoryId

) {}
