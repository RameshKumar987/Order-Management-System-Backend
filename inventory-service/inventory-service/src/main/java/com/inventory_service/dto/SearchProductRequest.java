package com.inventory_service.dto;

/**
 * DTO for searching products by name or SKU
 */
public record SearchProductRequest(
        String searchTerm,
        Integer minStock,
        Integer maxStock
) {}

