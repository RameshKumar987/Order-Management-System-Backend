package com.inventory_service.controller;

import com.inventory_service.dto.*;
import com.inventory_service.entity.Inventory;
import com.inventory_service.entity.Product;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService service;

    @PutMapping("/products/{id}/stock")
    public void updateStock(
            @PathVariable Long id,
            @RequestBody UpdateStockRequest req){

        service.updateStock(id, req.stock());
    }

    @GetMapping("/products/{id}/stock")
    public Inventory getStock(
            @PathVariable Long id){

        return service.getStock(id);
    }

    @PostMapping("/reserve")
    public void reserveStock(
            @RequestBody ReserveStockRequest req){

        service.reserveStock(req);
    }

    /**
     * Release reserved stock back to available stock
     *
     * @param request Contains orderId, productId, and quantity to release
     * @return 200 OK - Stock released successfully
     *         400 BAD REQUEST - Invalid input or insufficient reserved stock
     *         404 NOT FOUND - Product/Inventory not found
     *         500 INTERNAL SERVER ERROR - Unexpected error
     */
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<String>> releaseStock(
            @RequestBody ReleaseStockRequest request) {
        try {
            log.info("Release stock request - Order: {}, Product: {}, Quantity: {}",
                    request.orderId(), request.productId(), request.quantity());

            service.releaseStock(request);

            return ResponseEntity.ok(
                    ApiResponse.success("Stock released successfully",
                            "Successfully released " + request.quantity() + " units of product " + request.productId())
            );

        } catch(ValidationException e) {
            log.warn("Validation error during stock release: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(ResourceNotFoundException e) {
            log.warn("Resource not found during stock release: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));

        } catch(Exception e) {
            log.error("Unexpected error during stock release: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Deduct stock directly without reservation
     *
     * @param request Contains productId, quantity, and reason for deduction
     * @return 200 OK - Stock deducted successfully
     *         400 BAD REQUEST - Invalid input or insufficient stock
     *         404 NOT FOUND - Product/Inventory not found
     *         500 INTERNAL SERVER ERROR - Unexpected error
     */
    @PostMapping("/deduct")
    public ResponseEntity<ApiResponse<String>> deductStock(
            @RequestBody DeductStockRequest request) {
        try {
            log.info("Deduct stock request - Product: {}, Quantity: {}, Reason: {}",
                    request.productId(), request.quantity(), request.reason());

            service.deductStock(request);

            return ResponseEntity.ok(
                    ApiResponse.success("Stock deducted successfully",
                            "Successfully deducted " + request.quantity() + " units of product " + request.productId())
            );

        } catch(ValidationException e) {
            log.warn("Validation error during stock deduction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(ResourceNotFoundException e) {
            log.warn("Resource not found during stock deduction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));

        } catch(Exception e) {
            log.error("Unexpected error during stock deduction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Search products by name or description
     *
     * @param searchTerm Search keyword (optional query param)
     * @param minStock Minimum stock level filter (optional)
     * @param maxStock Maximum stock level filter (optional)
     * @return 200 OK - List of matching products
     *         400 BAD REQUEST - Invalid search parameters
     *         500 INTERNAL SERVER ERROR - Unexpected error
     */
    @GetMapping("/products/search")
    public ResponseEntity<ApiResponse<List<Product>>> searchProducts(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock) {
        try {
            log.info("Search products request - searchTerm: {}, minStock: {}, maxStock: {}",
                    searchTerm, minStock, maxStock);

            SearchProductRequest request = new SearchProductRequest(searchTerm, minStock, maxStock);
            List<Product> products = service.searchProducts(request);

            return ResponseEntity.ok(
                    ApiResponse.success(products,
                            "Found " + products.size() + " product(s) matching search criteria")
            );

        } catch(ValidationException e) {
            log.warn("Validation error during product search: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(Exception e) {
            log.error("Unexpected error during product search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }
}
