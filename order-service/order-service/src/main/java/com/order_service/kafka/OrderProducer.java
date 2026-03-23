package com.order_service.kafka;

import com.order_service.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderDto orderDto){
        try {
            log.info("📤 Publishing order event to Kafka: Order ID = {}, Items count = {}",
                    orderDto.id(), orderDto.items() != null ? orderDto.items().size() : 0);

            kafkaTemplate.send("order-events", orderDto);

            log.info("✅ Order event published successfully to order-events topic: Order ID = {}", orderDto.id());
        } catch (Exception e) {
            log.error("❌ Error publishing order event to Kafka: Order ID = {}, Error: {}",
                    orderDto.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish order event: " + e.getMessage(), e);
        }
    }
}

