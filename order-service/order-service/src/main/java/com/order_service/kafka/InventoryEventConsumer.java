package com.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_service.entity.Order;
import com.order_service.enums.OrderStatus;
import com.order_service.repository.OrderRepository;
import com.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "inventory-events", groupId = "order-inventory-group")
    @Transactional
    public void consumeInventoryEvent(String message) {
        try {
            log.info("📨 Received inventory event: {}", message);
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) objectMapper.readValue(message, Map.class);

            Long orderId = ((Number) event.get("orderId")).longValue();
            String status = (String) event.get("status");

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("⚠️ Order not found for ID: {}", orderId);
                return;
            }

            if ("VALIDATED".equals(status)) {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    log.warn("⚠️ Order {} was already CANCELLED. Ignoring inventory validation.", orderId);
                    // Stock release was already published by order cancellation.
                    return;
                }

                Double totalAmount = ((Number) event.get("totalAmount")).doubleValue();
                
                log.info("✅ Order VALIDATED by inventory. Updating prices for Order ID: {}", orderId);
                order.setStatus(OrderStatus.PAYMENT_PENDING);
                order.setTotalAmount(totalAmount);
                
                // Update individual item prices
                if (event.containsKey("items")) {
                    java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) event.get("items");
                    for (Map<String, Object> itemData : items) {
                        Long productId = ((Number) itemData.get("productId")).longValue();
                        Double price = ((Number) itemData.get("price")).doubleValue();
                        order.getItems().stream()
                             .filter(i -> i.getProductId().equals(productId))
                             .findFirst()
                             .ifPresent(i -> i.setPrice(price));
                    }
                }
                
                orderRepository.save(order);
                orderService.saveHistory(order);
                log.info("✅ Order status updated to PAYMENT_PENDING. Total Amount: ₹{}", totalAmount);

            } else if ("FAILED".equals(status)) {
                log.warn("❌ Order FAILED inventory validation. Status: {}", status);
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                orderService.saveHistory(order);
            }

        } catch (Exception e) {
            log.error("❌ Error processing inventory event: {}", e.getMessage(), e);
        }
    }
}
