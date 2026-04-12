package com.inventory_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory_service.entity.Inventory;
import com.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCompensationConsumer {

    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "inventory-compensation", groupId = "inventory-compensation-group")
    public void handleCompensation(String message) {
        try {
            log.info("🔄 Received compensation event: {}", message);

            Map<String, Object> compensationEvent = objectMapper.readValue(message, Map.class);

            Long orderId = ((Number) compensationEvent.get("orderId")).longValue();
            String action = (String) compensationEvent.get("action");

            if ("RELEASE_STOCK".equals(action)) {
                releaseStock(compensationEvent);
                log.info("✅ Stock released for Order ID: {}", orderId);
            } else {
                log.warn("⚠️  Unknown compensation action: {}", action);
            }

        } catch (Exception e) {
            log.error("❌ Error processing compensation event: {}", e.getMessage());
        }
    }

    private void releaseStock(Map<String, Object> compensationEvent) {
        try {
            Long orderId = ((Number) compensationEvent.get("orderId")).longValue();

            // Process items if present
            if (compensationEvent.containsKey("items")) {
                java.util.List<Map<String, Object>> items =
                    (java.util.List<Map<String, Object>>) compensationEvent.get("items");

                for (Map<String, Object> item : items) {
                    Long productId = ((Number) item.get("productId")).longValue();
                    Integer quantity = ((Number) item.get("quantity")).intValue();

                    Inventory inventory = inventoryRepository.findById(productId).orElse(null);
                    if (inventory != null) {
                        // Restore available stock and reduce reserved stock
                        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);
                        inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - quantity));
                        inventoryRepository.save(inventory);

                        log.info("✅ Stock released: Product ID = {}, Quantity = {}, Available = {}, Reserved = {}",
                                productId, quantity, inventory.getAvailableStock(), inventory.getReservedStock());
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Error releasing stock: {}", e.getMessage());
            throw new RuntimeException("Failed to release stock: " + e.getMessage());
        }
    }
}

