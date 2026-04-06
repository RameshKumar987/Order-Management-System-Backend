package com.order_service.kafka;

import com.order_service.dto.OrderDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    @Qualifier("kafkaTemplateObject")
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishOrderCreated(OrderDto orderDto) {
        publishOrderEvent(orderDto, "order-events");
    }

    public void publishOrderCancelled(OrderDto orderDto) {
        publishOrderEvent(orderDto, "order-events");

        // Also publish to compensation for safety
        try {
            Map<String, Object> compensationEvent = new HashMap<>();
            compensationEvent.put("orderId", orderDto.id());
            compensationEvent.put("action", "RELEASE_STOCK");
            compensationEvent.put("items", orderDto.items());
            String json = objectMapper.writeValueAsString(compensationEvent);
            kafkaTemplate.send("inventory-compensation", json);
            log.info("🔄 Compensation event published for Order ID: {}", orderDto.id());
        } catch (Exception e) {
            log.error("❌ Error publishing compensation event: {}", e.getMessage());
        }
    }

    private void publishOrderEvent(OrderDto orderDto, String topic) {
        try {
            log.info("📤 Publishing order event to Kafka topic {}: Order ID = {}, Status = {}",
                    topic, orderDto.id(), orderDto.status());

            Map<String, Object> event = new HashMap<>();
            event.put("orderId", orderDto.id());
            event.put("userId", orderDto.userId());
            event.put("status", orderDto.status());
            event.put("items", orderDto.items());
            event.put("deliveryAddress", orderDto.deliveryAddress());
            event.put("deliveryCity", orderDto.deliveryCity());
            event.put("deliveryLat", orderDto.deliveryLat());
            event.put("deliveryLng", orderDto.deliveryLng());

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, json);

            log.info("✅ Order event published successfully to {}: Order ID = {}", topic, orderDto.id());
        } catch (Exception e) {
            log.error("❌ Error publishing order event to Kafka: Order ID = {}, Error: {}",
                    orderDto.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish order event: " + e.getMessage(), e);
        }
    }
}
