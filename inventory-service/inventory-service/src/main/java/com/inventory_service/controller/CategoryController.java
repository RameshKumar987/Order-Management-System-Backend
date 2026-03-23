package com.inventory_service.controller;

import com.inventory_service.dto.CreateCategoryRequest;
import com.inventory_service.dto.ApiResponse;
import com.inventory_service.entity.Category;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService service;

    /**
     * Create a new category
     * @param req the category creation request
     * @return created category with 200 status
     */
    @PostMapping
    public Category create(@RequestBody CreateCategoryRequest req){
        log.info("Creating category: {}", req);
        return service.create(req);
    }

    /**
     * Get all categories
     * @return list of all categories
     */
    @GetMapping
    public List<Category> getAll(){
        return service.getAll();
    }

    /**
     * Get category by ID
     * @param id Category ID
     * @return 200 OK with category data
     *         400 BAD REQUEST if id is invalid
     *         404 NOT FOUND if category doesn't exist
     *         500 INTERNAL SERVER ERROR for unexpected errors
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable Long id){
        try {
            log.info("Fetching category with id: {}", id);
            Category category = service.getById(id);
            return ResponseEntity.ok(ApiResponse.success(category, "Category fetched successfully"));

        } catch(ValidationException e){
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(ResourceNotFoundException e){
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));

        } catch(Exception e){
            log.error("Unexpected error while fetching category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Update category by ID
     * @param id Category ID
     * @param req Update request with name and description
     * @return 200 OK with updated category data
     *         400 BAD REQUEST if validation fails
     *         404 NOT FOUND if category doesn't exist
     *         500 INTERNAL SERVER ERROR for unexpected errors
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(
            @PathVariable Long id,
            @RequestBody CreateCategoryRequest req){
        try {
            log.info("Updating category with id: {} with data: {}", id, req);
            Category updatedCategory = service.update(id, req);
            return ResponseEntity.ok(ApiResponse.success(updatedCategory, "Category updated successfully"));

        } catch(ValidationException e){
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch(ResourceNotFoundException e){
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));

        } catch(Exception e){
            log.error("Unexpected error while updating category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Delete category by ID
     * @param id Category ID
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}
