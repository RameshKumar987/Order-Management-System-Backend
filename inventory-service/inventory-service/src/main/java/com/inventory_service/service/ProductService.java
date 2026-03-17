package com.inventory_service.service;

import com.inventory_service.dto.CreateProductRequest;
import com.inventory_service.entity.Category;
import com.inventory_service.entity.Product;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.repository.CategoryRepository;
import com.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

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
}
