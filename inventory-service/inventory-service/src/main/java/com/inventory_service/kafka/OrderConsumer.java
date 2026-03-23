package com.inventory_service.kafka;

import com.inventory_service.dto.OrderDto;
import com.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "order-events", groupId = "inventory-group-v3")
    public void consumeOrderCreated(OrderDto order) {
        try {
            if (order == null) {
                log.error("❌ Received null order from Kafka");
                return;
            }

            log.info("📨 Received order event: Order ID = {}, User ID = {}, Status = {}, Items count = {}",
                    order.id(), order.userId(), order.status(), order.items() != null ? order.items().size() : 0);

            // Process order and validate stock
            productService.processOrder(order);

            log.info("✅ Order processed successfully: Order ID = {}", order.id());

        } catch (Exception e) {
            log.error("❌ Error processing order ID: {}: {}",
                    order != null ? order.id() : "UNKNOWN", e.getMessage());
            // Don't re-throw - failure message already sent to Kafka by ProductService.processOrder()
        }
    }
}