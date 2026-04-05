package com.inventory_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory_service.dto.OrderDto;
import com.inventory_service.dto.OrderItemDto;
import com.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-events", groupId = "inventory-group-v3")
    public void consumeOrderCreated(String message) {
        try {
            if (message == null || message.isEmpty()) {
                log.error("❌ Received null or empty message from Kafka");
                return;
            }

            log.info("📨 Received raw order event message: {}", message);

            // Parse JSON string to Map
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);

            Long orderId = ((Number) eventData.get("orderId")).longValue();
            Long userId = ((Number) eventData.get("userId")).longValue();
            String status = (String) eventData.get("status");

            if (!"PENDING".equalsIgnoreCase(status)) {
                log.info("⏩ Ignoring order event with status {}. Only processing PENDING orders for initial stock reservation.", status);
                return;
            }

            // Convert items from Map to OrderItemDto list
            List<OrderItemDto> items = new ArrayList<>();
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) eventData.get("items");

            if (itemsData != null) {
                for (Map<String, Object> itemMap : itemsData) {
                    Long productId = ((Number) itemMap.get("productId")).longValue();
                    Integer quantity = ((Number) itemMap.get("quantity")).intValue();
                    Double price = ((Number) itemMap.getOrDefault("price", 0.0)).doubleValue();

                    items.add(new OrderItemDto(productId, quantity, price));
                }
            }

            log.info("📨 Parsed order event: Order ID = {}, User ID = {}, Status = {}, Items count = {}",
                    orderId, userId, status, items.size());

            // Create OrderDto
            OrderDto order = new OrderDto(orderId, userId, status, items);

            // Process order and validate stock
            productService.processOrder(order);

            log.info("✅ Order processed successfully: Order ID = {}", orderId);

        } catch (Exception e) {
            log.error("❌ Error processing order event: {}", e.getMessage(), e);
            // Don't re-throw - failure message already sent to Kafka by ProductService.processOrder()
        }
    }
}