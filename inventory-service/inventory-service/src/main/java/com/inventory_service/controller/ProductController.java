package com.inventory_service.controller;

import com.inventory_service.dto.ApiResponse;
import com.inventory_service.dto.CreateProductRequest;
import com.inventory_service.entity.Product;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService service;

    /**
     * Create a new product
     */
    @PostMapping
    public Product create(
            @RequestBody CreateProductRequest req){
        log.info("Creating product: {}", req);
        return service.create(req);
    }

    /**
     * Get all products
     */
    @GetMapping
    public List<Product> getAll(){
        return service.getAll();
    }

    /**
     * Get product by ID
     * @param id Product ID
     * @return 200 OK with product data
     *         400 BAD REQUEST if id is invalid
     *         404 NOT FOUND if product doesn't exist
     *         500 INTERNAL SERVER ERROR for unexpected errors
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(
            @PathVariable Long id){
        try {
            log.info("Fetching product with id: {}", id);
            Product product = service.getById(id);
            return ResponseEntity.ok(ApiResponse.success(product, "Product fetched successfully"));

        } catch(ValidationException e){
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(ResourceNotFoundException e){
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));

        } catch(Exception e){
            log.error("Unexpected error while fetching product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get products by category
     */
    @GetMapping("/category/{id}")
    public List<Product> byCategory(
            @PathVariable Long id){
        return service.getByCategory(id);
    }

    /**
     * Update product by ID
     * @param id Product ID
     * @param req Update request with name, description, price, categoryId
     * @return 200 OK with updated product data
     *         400 BAD REQUEST if validation fails
     *         404 NOT FOUND if product doesn't exist
     *         500 INTERNAL SERVER ERROR for unexpected errors
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(
            @PathVariable Long id,
            @RequestBody CreateProductRequest req){
        try {
            log.info("Updating product with id: {} with data: {}", id, req);
            Product updatedProduct = service.update(id, req);
            return ResponseEntity.ok(ApiResponse.success(updatedProduct, "Product updated successfully"));

        } catch(ValidationException e){
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(ResourceNotFoundException e){
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));

        } catch(Exception e){
            log.error("Unexpected error while updating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Delete product by ID
     */
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id){

        service.delete(id);
    }
}
