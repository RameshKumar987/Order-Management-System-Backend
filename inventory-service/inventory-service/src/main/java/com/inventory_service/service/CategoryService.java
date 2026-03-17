package com.inventory_service.service;

import com.inventory_service.dto.CreateCategoryRequest;
import com.inventory_service.entity.Category;
import com.inventory_service.exception.ResourceNotFoundException;
import com.inventory_service.exception.ValidationException;
import com.inventory_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Validate category ID
     * @param id the id to validate
     * @throws ValidationException if id is invalid
     */
    private void validateCategoryId(Long id) {
        if(id == null || id <= 0) {
            throw new ValidationException("id", "Invalid category ID. ID must be a positive number");
        }
    }

    /**
     * Validate category request
     * @param request the request to validate
     * @throws ValidationException if request is invalid
     */
    private void validateCategoryRequest(CreateCategoryRequest request) {
        if(request == null) {
            throw new ValidationException("request", "Category request cannot be null");
        }

        if(request.name() == null || request.name().trim().isEmpty()) {
            throw new ValidationException("name", "Category name cannot be empty");
        }

        if(request.name().length() > 100) {
            throw new ValidationException("name", "Category name cannot exceed 100 characters");
        }
    }

    /**
     * Create a new category
     * @param request the category creation request
     * @return created category
     * @throws ValidationException if request is invalid or name already exists
     */
    public Category create(CreateCategoryRequest request){
        log.info("Creating new category with name: {}", request.name());

        validateCategoryRequest(request);

        if(categoryRepository.existsByName(request.name())) {
            log.warn("Category with name '{}' already exists", request.name());
            throw new ValidationException("name", "Category with name '" + request.name() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", savedCategory.getId());
        return savedCategory;
    }

    /**
     * Get all categories
     * @return list of all categories
     */
    public List<Category> getAll(){
        log.info("Fetching all categories");
        return categoryRepository.findAll();
    }

    /**
     * Get category by ID
     * @param id the category id
     * @return category if found
     * @throws ValidationException if id is invalid
     * @throws ResourceNotFoundException if category not found
     */
    public Category getById(Long id){
        log.info("Fetching category with id: {}", id);

        validateCategoryId(id);

        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", id);
                    return new ResourceNotFoundException("Category", "id", id);
                });
    }

    /**
     * Update an existing category
     * @param id the category id to update
     * @param request the update request
     * @return updated category
     * @throws ValidationException if request is invalid or name already exists
     * @throws ResourceNotFoundException if category not found
     */
    public Category update(Long id, CreateCategoryRequest request){
        log.info("Updating category with id: {} with data: {}", id, request);

        validateCategoryId(id);
        validateCategoryRequest(request);

        Category category = getById(id);

        // Check if new name already exists (but not same as current name)
        if(!category.getName().equals(request.name()) &&
           categoryRepository.existsByName(request.name())) {
            log.warn("Category with name '{}' already exists", request.name());
            throw new ValidationException("name", "Category with name '" + request.name() + "' already exists");
        }

        category.setName(request.name());
        category.setDescription(request.description());

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", id);
        return updatedCategory;
    }

    /**
     * Delete a category by ID
     * @param id the category id
     * @throws ValidationException if id is invalid
     * @throws ResourceNotFoundException if category not found
     */
    public void delete(Long id){
        log.info("Deleting category with id: {}", id);

        validateCategoryId(id);

        if(!categoryRepository.existsById(id)) {
            log.warn("Category not found with id: {}", id);
            throw new ResourceNotFoundException("Category", "id", id);
        }

        categoryRepository.deleteById(id);
        log.info("Category deleted successfully with id: {}", id);
    }
}
