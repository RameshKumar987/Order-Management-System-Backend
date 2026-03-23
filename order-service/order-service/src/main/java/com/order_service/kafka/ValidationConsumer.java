
package com.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-validation-topic", groupId = "order-group")
    public void consumeValidationResult(String message) {
        try {
            log.info("📨 Received validation message from Kafka: {}", message);

            Map<String, Object> validationResult = objectMapper.readValue(message, Map.class);

            Long orderId = ((Number) validationResult.get("orderId")).longValue();
            String status = (String) validationResult.get("status");

            log.info("✅ Parsed validation result: Order ID = {}, Status = {}", orderId, status);

            if ("FAILED".equals(status)) {
                String error = (String) validationResult.get("error");
                log.warn("⚠️  Order validation FAILED for Order ID: {}, Reason: {}", orderId, error);
            }

            orderService.updateOrderFromValidation(validationResult);

            if ("VALIDATED".equals(status)) {
                log.info("✅ Order updated successfully from validation: Order ID = {}", orderId);
            } else if ("FAILED".equals(status)) {
                log.warn("⚠️  Order marked as FAILED: Order ID = {}", orderId);
            }

        } catch (ClassCastException | NullPointerException e) {
            log.error("❌ Type conversion error while parsing Kafka message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error processing validation message from Kafka: {}", e.getMessage());
        }
    }
}

