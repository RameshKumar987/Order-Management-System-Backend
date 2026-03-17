package com.inventory_service.service;

import com.inventory_service.dto.DeductStockRequest;
import com.inventory_service.dto.ReleaseStockRequest;
import com.inventory_service.dto.ReserveStockRequest;
import com.inventory_service.dto.SearchProductRequest;
import com.inventory_service.entity.Inventory;
import com.inventory_service.entity.Product;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.kafka.producer.InventoryEventProducer;
import com.inventory_service.repository.InventoryRepository;
import com.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryEventProducer producer;

    /**
     * Validate product ID
     */
    private void validateProductId(Long productId) {
        if(productId == null || productId <= 0) {
            throw new ValidationException("productId", "Invalid product ID. ID must be a positive number");
        }
    }

    /**
     * Validate quantity
     */
    private void validateQuantity(int quantity) {
        if(quantity <= 0) {
            throw new ValidationException("quantity", "Quantity must be greater than 0");
        }
    }

    /**
     * Update stock for a product
     */
    public void updateStock(Long productId, int stock){
        log.info("Updating stock for product {}: {}", productId, stock);
        validateProductId(productId);

        if(stock < 0) {
            throw new ValidationException("stock", "Stock cannot be negative");
        }

        Inventory inventory = inventoryRepository
                .findById(productId)
                .orElse(new Inventory());

        inventory.setProductId(productId);
        inventory.setAvailableStock(stock);

        inventoryRepository.save(inventory);
        log.info("Stock updated successfully for product {}", productId);
    }

    /**
     * Get stock information for a product
     */
    public Inventory getStock(Long productId){
        log.info("Fetching stock for product {}", productId);
        validateProductId(productId);

        return inventoryRepository
                .findById(productId)
                .orElseThrow(() -> {
                    log.warn("Inventory not found for product {}", productId);
                    return new ResourceNotFoundException("Inventory", "productId", productId);
                });
    }

    /**
     * Reserve stock for an order
     */
    public void reserveStock(ReserveStockRequest request){
        log.info("Reserving stock - Order: {}, Product: {}, Quantity: {}",
                request.orderId(), request.productId(), request.quantity());

        validateProductId(request.productId());
        validateQuantity(request.quantity());

        Inventory inventory = inventoryRepository
                .findById(request.productId())
                .orElseThrow(() -> {
                    log.warn("Inventory not found for product {}", request.productId());
                    return new ResourceNotFoundException("Inventory", "productId", request.productId());
                });

        if(inventory.getAvailableStock() < request.quantity()){
            log.warn("Insufficient stock for product {}. Available: {}, Requested: {}",
                    request.productId(), inventory.getAvailableStock(), request.quantity());
            throw new ValidationException("stock", "Insufficient stock available. Available: " +
                    inventory.getAvailableStock() + ", Requested: " + request.quantity());
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - request.quantity());
        inventory.setReservedStock(inventory.getReservedStock() + request.quantity());

        inventoryRepository.save(inventory);
        log.info("Stock reserved successfully for order {}", request.orderId());

        producer.sendInventoryReservedEvent(request);
    }

    /**
     * Release reserved stock back to available stock
     */
    public void releaseStock(ReleaseStockRequest request){
        log.info("Releasing stock - Order: {}, Product: {}, Quantity: {}",
                request.orderId(), request.productId(), request.quantity());

        validateProductId(request.productId());
        validateQuantity(request.quantity());

        Inventory inventory = inventoryRepository
                .findById(request.productId())
                .orElseThrow(() -> {
                    log.warn("Inventory not found for product {}", request.productId());
                    return new ResourceNotFoundException("Inventory", "productId", request.productId());
                });

        if(inventory.getReservedStock() < request.quantity()){
            log.warn("Cannot release stock for product {}. Reserved: {}, Requested: {}",
                    request.productId(), inventory.getReservedStock(), request.quantity());
            throw new ValidationException("stock", "Cannot release more stock than reserved. Reserved: " +
                    inventory.getReservedStock() + ", Requested: " + request.quantity());
        }

        inventory.setReservedStock(inventory.getReservedStock() - request.quantity());
        inventory.setAvailableStock(inventory.getAvailableStock() + request.quantity());

        inventoryRepository.save(inventory);
        log.info("Stock released successfully for order {}", request.orderId());
    }

    /**
     * Deduct stock directly without reservation
     */
    public void deductStock(DeductStockRequest request){
        log.info("Deducting stock - Product: {}, Quantity: {}, Reason: {}",
                request.productId(), request.quantity(), request.reason());

        validateProductId(request.productId());
        validateQuantity(request.quantity());

        if(request.reason() == null || request.reason().trim().isEmpty()) {
            throw new ValidationException("reason", "Reason for deduction cannot be empty");
        }

        Inventory inventory = inventoryRepository
                .findById(request.productId())
                .orElseThrow(() -> {
                    log.warn("Inventory not found for product {}", request.productId());
                    return new ResourceNotFoundException("Inventory", "productId", request.productId());
                });

        if(inventory.getAvailableStock() < request.quantity()){
            log.warn("Insufficient stock for product {}. Available: {}, Requested: {}",
                    request.productId(), inventory.getAvailableStock(), request.quantity());
            throw new ValidationException("stock", "Insufficient stock available for deduction. Available: " +
                    inventory.getAvailableStock() + ", Requested: " + request.quantity());
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - request.quantity());

        inventoryRepository.save(inventory);
        log.info("Stock deducted successfully for product {} with reason: {}", request.productId(), request.reason());
    }

    /**
     * Search products by name or description with optional stock filters
     */
    public List<Product> searchProducts(SearchProductRequest request){
        log.info("Searching products with searchTerm: {}, minStock: {}, maxStock: {}",
                request.searchTerm(), request.minStock(), request.maxStock());

        if(request.searchTerm() == null || request.searchTerm().trim().isEmpty()) {
            throw new ValidationException("searchTerm", "Search term cannot be empty");
        }

        if(request.searchTerm().length() > 100) {
            throw new ValidationException("searchTerm", "Search term cannot exceed 100 characters");
        }

        List<Product> products = productRepository.searchByNameOrDescription(request.searchTerm());

        log.info("Found {} products matching search term: {}", products.size(), request.searchTerm());
        return products;
    }
}
