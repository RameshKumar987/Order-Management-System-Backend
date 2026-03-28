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

    public void publishOrderCreated(OrderDto orderDto){
        try {
            log.info("📤 Publishing order event to Kafka: Order ID = {}, Items count = {}",
                    orderDto.id(), orderDto.items() != null ? orderDto.items().size() : 0);

            Map<String, Object> event = new HashMap<>();
            event.put("orderId", orderDto.id());
            event.put("userId", orderDto.userId());
            event.put("status", orderDto.status());
            event.put("items", orderDto.items());

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-events", json);

            log.info("✅ Order event published successfully to order-events topic: Order ID = {}", orderDto.id());
        } catch (Exception e) {
            log.error("❌ Error publishing order event to Kafka: Order ID = {}, Error: {}",
                    orderDto.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish order event: " + e.getMessage(), e);
        }
    }
}

