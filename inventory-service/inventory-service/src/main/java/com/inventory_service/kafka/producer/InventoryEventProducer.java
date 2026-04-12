package com.inventory_service.kafka.producer;

import com.inventory_service.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryEventProducer {

    @Qualifier("kafkaTemplateObject")
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendInventoryReservedEvent(OrderEvent event){
        event.setEventType("INVENTORY_RESERVED");
        kafkaTemplate.send("inventory-events", event);
    }

    public void sendInventoryFailedEvent(OrderEvent event) {
        event.setEventType("INVENTORY_FAILED");
        kafkaTemplate.send("inventory-failed-topic", event);
    }
}