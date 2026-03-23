package com.inventory_service.controller;

import com.inventory_service.entity.ProductImage;
import com.inventory_service.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService imageService;

    // URL based image
    @PostMapping("/{id}/images/url")
    public ProductImage addImageUrl(
            @PathVariable Long id,
            @RequestParam String url) {

        return imageService.addImageUrl(id, url);
    }

    // Multipart upload
    @PostMapping("/{id}/images/upload")
    public ProductImage uploadImage(
            @PathVariable Long id,
            @RequestParam MultipartFile file) {

        return imageService.uploadImage(id, file);
    }

    // Get images
    @GetMapping("/{id}/images")
    public List<ProductImage> getImages(
            @PathVariable Long id) {

        return imageService.getImages(id);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {

        try {
            imageService.deleteImage(id, imageId);
            return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}
