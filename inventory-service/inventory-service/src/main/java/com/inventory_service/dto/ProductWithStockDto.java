package com.inventory_service.dto;

import com.inventory_service.entity.Category;
import java.util.List;

public record ProductWithStockDto(
    Long id,
    String name,
    String description,
    double price,
    Category category,
    int stock,
    String status,
    List<String> imageUrls
) {}
