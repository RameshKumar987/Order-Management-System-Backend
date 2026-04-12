package com.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "payment-events", groupId = "order-payment-group")
    public void consumePaymentEvent(String message) {
        try {
            log.info("📨 Received payment event from Kafka: {}", message);

            Map<String, Object> paymentEvent = objectMapper.readValue(message, Map.class);

            Long orderId = ((Number) paymentEvent.get("orderId")).longValue();
            String status = (String) paymentEvent.get("status");

            log.info("✅ Parsed payment event: Order ID = {}, Status = {}", orderId, status);

            if ("PAYMENT_SUCCESS".equals(status)) {
                log.info("✅ Payment successful for Order ID: {}", orderId);
                orderService.confirmOrder(orderId);
            } else if ("PAYMENT_FAILED".equals(status)) {
                log.warn("⚠️  Payment failed for Order ID: {}", orderId);
                orderService.cancelOrder(orderId);
                publishCompensation(orderId);
            } else {
                log.warn("⚠️  Unknown payment status: {}", status);
            }

        } catch (ClassCastException | NullPointerException e) {
            log.error("❌ Type conversion error while parsing Kafka message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error processing payment event from Kafka: {}", e.getMessage());
        }
    }

    private void publishCompensation(Long orderId) {
        try {
            Map<String, Object> compensationEvent = new HashMap<>();
            compensationEvent.put("orderId", orderId);
            compensationEvent.put("action", "RELEASE_STOCK");

            // Items will be populated by OrderService if needed
            compensationEvent.put("items", java.util.Collections.emptyList());

            String json = objectMapper.writeValueAsString(compensationEvent);
            kafkaTemplate.send("inventory-compensation", json);

            log.info("🔄 Compensation event published for Order ID: {}", orderId);
        } catch (Exception e) {
            log.error("❌ Error publishing compensation event: {}", e.getMessage());
        }
    }
}

