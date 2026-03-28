package com.inventory_service.service;

import com.inventory_service.dto.CreateProductRequest;
import com.inventory_service.dto.OrderDto;
import com.inventory_service.dto.OrderItemDto;
import com.inventory_service.entity.Category;
import com.inventory_service.entity.Inventory;
import com.inventory_service.entity.Product;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.repository.CategoryRepository;
import com.inventory_service.repository.InventoryRepository;
import com.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate product ID
     */
    private void validateProductId(Long id) {
        if(id == null || id <= 0) {
            throw new ValidationException("id", "Invalid product ID. ID must be a positive number");
        }
    }

    /**
     * Validate product request
     */
    private void validateProductRequest(CreateProductRequest request) {
        if(request == null) {
            throw new ValidationException("request", "Product request cannot be null");
        }

        if(request.name() == null || request.name().trim().isEmpty()) {
            throw new ValidationException("name", "Product name cannot be empty");
        }

        if(request.name().length() > 100) {
            throw new ValidationException("name", "Product name cannot exceed 100 characters");
        }

        if(request.price() <= 0) {
            throw new ValidationException("price", "Product price must be greater than 0");
        }

        if(request.categoryId() == null || request.categoryId() <= 0) {
            throw new ValidationException("categoryId", "Invalid category ID. ID must be a positive number");
        }
    }

    /**
     * Create a new product
     */
    public Product create(CreateProductRequest request){
        log.info("Creating product: {}", request.name());
        validateProductRequest(request);

        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", request.categoryId());
                    return new ResourceNotFoundException("Category", "id", request.categoryId());
                });

        Product p = new Product();
        p.setName(request.name());
        p.setDescription(request.description());
        p.setPrice(request.price());
        p.setCategory(category);

        Product savedProduct = productRepository.save(p);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        return savedProduct;
    }

    /**
     * Get all products
     */
    public List<Product> getAll(){
        log.info("Fetching all products");
        return productRepository.findAll();
    }

    /**
     * Get product by ID
     */
    public Product getById(Long id){
        log.info("Fetching product with id: {}", id);
        validateProductId(id);

        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", id);
                    return new ResourceNotFoundException("Product", "id", id);
                });
    }

    /**
     * Get products by category
     */
    public List<Product> getByCategory(Long id){
        log.info("Fetching products by category: {}", id);
        validateProductId(id);

        return productRepository.findByCategoryId(id);
    }

    /**
     * Update an existing product
     */
    public Product update(Long id, CreateProductRequest request) {
        log.info("Updating product with id: {} with data: {}", id, request);

        validateProductId(id);
        validateProductRequest(request);

        Product product = getById(id);

        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", request.categoryId());
                    return new ResourceNotFoundException("Category", "id", request.categoryId());
                });

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with id: {}", id);
        return updatedProduct;
    }

    /**
     * Delete a product by ID
     */
    public void delete(Long id){
        log.info("Deleting product with id: {}", id);
        validateProductId(id);

        if(!productRepository.existsById(id)) {
            log.warn("Product not found with id: {}", id);
            throw new ResourceNotFoundException("Product", "id", id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted successfully with id: {}", id);
    }

    public void processOrder(OrderDto order) {
        log.info("📦 Processing order: Order ID = {}, Items count = {}", order.id(), order.items().size());

        try {
            // 1. Validate all products exist before processing
            for (OrderItemDto item : order.items()) {
                if (!productRepository.existsById(item.productId())) {
                    String errorMsg = String.format("Product not found with ID: %d", item.productId());
                    log.warn("⚠️  {}", errorMsg);
                    publishValidationFailure(order.id(), errorMsg);
                    return;
                }
            }

            // 2. Validate stock availability for all items
            for (OrderItemDto item : order.items()) {
                Inventory inventory = inventoryRepository.findById(item.productId())
                        .orElse(null);

                if (inventory == null) {
                    String errorMsg = String.format("Inventory not found for product ID: %d", item.productId());
                    log.warn("⚠️  {}", errorMsg);
                    publishValidationFailure(order.id(), errorMsg);
                    return;
                }

                if (inventory.getAvailableStock() < item.quantity()) {
                    String errorMsg = String.format(
                        "Insufficient stock for product %d. Available: %d, Requested: %d",
                        item.productId(), inventory.getAvailableStock(), item.quantity());
                    log.warn("⚠️  {}", errorMsg);
                    publishValidationFailure(order.id(), errorMsg);
                    return;
                }
            }

            // 3. Calculate total amount
            double totalAmount = order.items().stream()
                    .mapToDouble(item -> {
                        Product product = productRepository.findById(item.productId()).orElse(null);
                        if (product == null) {
                            throw new RuntimeException("Product not found: " + item.productId());
                        }
                        double itemTotal = product.getPrice() * item.quantity();
                        log.debug("Item: Product ID = {}, Quantity = {}, Price = {}, Total = {}",
                                item.productId(), item.quantity(), product.getPrice(), itemTotal);
                        return itemTotal;
                    })
                    .sum();

            log.info("✅ Order validation complete: Order ID = {}, Total Amount = {}", order.id(), totalAmount);

            // 4. Reserve stock for all items
            for (OrderItemDto item : order.items()) {
                Inventory inventory = inventoryRepository.findById(item.productId()).orElseThrow();
                inventory.setAvailableStock(inventory.getAvailableStock() - item.quantity());
                inventory.setReservedStock(inventory.getReservedStock() + item.quantity());
                inventoryRepository.save(inventory);
                log.info("✅ Stock reserved: Order ID = {}, Product ID = {}, Quantity = {}",
                        order.id(), item.productId(), item.quantity());
            }

            // 5. Publish validation success result
            publishValidationSuccess(order.id(), totalAmount);
            log.info("✅ Order validated successfully: Order ID = {}", order.id());

        } catch (Exception e) {
            String errorMsg = "Order processing failed: " + e.getMessage();
            log.error("❌ {}", errorMsg);
            publishValidationFailure(order.id(), errorMsg);
        }
    }

    private void publishValidationSuccess(Long orderId, Double totalAmount) {
        try {
            Map<String, Object> validationResult = new HashMap<>();
            validationResult.put("orderId", orderId);
            validationResult.put("totalAmount", totalAmount);
            validationResult.put("status", "VALIDATED");

            String json = objectMapper.writeValueAsString(validationResult);
            kafkaTemplate.send("inventory-events", json);
            log.info("✅ Validation success published for Order ID: {}", orderId);
        } catch (Exception e) {
            log.error("❌ Error publishing validation success: {}", e.getMessage());
        }
    }

    private void publishValidationFailure(Long orderId, String errorMessage) {
        try {
            Map<String, Object> failureResult = new HashMap<>();
            failureResult.put("orderId", orderId);
            failureResult.put("totalAmount", 0.0);
            failureResult.put("status", "FAILED");
            failureResult.put("error", errorMessage);

            String json = objectMapper.writeValueAsString(failureResult);
            kafkaTemplate.send("inventory-events", json);
            log.warn("⚠️  Validation failure published for Order ID: {}, Error: {}", orderId, errorMessage);
        } catch (Exception e) {
            log.error("❌ Error publishing validation failure: {}", e.getMessage());
        }
    }
}