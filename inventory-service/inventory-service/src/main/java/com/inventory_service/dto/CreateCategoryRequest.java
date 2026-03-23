package com.inventory_service.dto;

public record CreateCategoryRequest(
        String name,
        String description
) {}
