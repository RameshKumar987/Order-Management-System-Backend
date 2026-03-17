package com.inventory_service.repository;

import com.inventory_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product,Long> {

    List<Product> findByCategoryId(Long id);

    /**
     * Search products by name (case-insensitive)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Search products by price range
     */
    List<Product> findByPriceBetween(double minPrice, double maxPrice);

    /**
     * Custom search query combining name and price
     */
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Product> searchByNameOrDescription(@Param("searchTerm") String searchTerm);
}
