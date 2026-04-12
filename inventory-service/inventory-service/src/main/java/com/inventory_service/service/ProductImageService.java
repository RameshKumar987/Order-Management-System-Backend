package com.inventory_service.service;

import com.inventory_service.entity.Product;
import com.inventory_service.entity.ProductImage;
import com.inventory_service.repository.ProductImageRepository;
import com.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;

    public ProductImage addImageUrl(Long productId, String url) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductImage image = new ProductImage();

        image.setImageUrl(url);
        image.setProduct(product);

        return imageRepository.save(image);
    }

    public ProductImage uploadImage(Long productId, MultipartFile file) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String imageUrl;
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            imageUrl = "data:" + file.getContentType() + ";base64," + base64Image;
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image file", e);
        }

        ProductImage image = new ProductImage();

        image.setImageUrl(imageUrl);
        image.setProduct(product);

        return imageRepository.save(image);
    }

    public List<ProductImage> getImages(Long productId) {
        return imageRepository.findByProductId(productId);
    }

    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new RuntimeException("Image does not belong to the specified product");
        }

        imageRepository.delete(image);
    }
}




