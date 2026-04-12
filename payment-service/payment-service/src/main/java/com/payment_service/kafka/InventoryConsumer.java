package com.payment_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "inventory-events", groupId = "payment-group")
    public void consumeInventoryEvent(String message) {

        log.info("🔥 Inventory Event Received: {}", message);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(message, Map.class);

            Long orderId = Long.valueOf(event.get("orderId").toString());
            Long userId = Long.valueOf(event.get("userId").toString());
            String status = event.get("status").toString();
            Double amount = Double.valueOf(event.get("totalAmount").toString());

            if (status.equals("VALIDATED")) {
                paymentService.autoCreatePayment(orderId, userId, amount);
            }

        } catch (Exception e) {
            log.error("❌ Error processing inventory event", e);
        }
    }
}
