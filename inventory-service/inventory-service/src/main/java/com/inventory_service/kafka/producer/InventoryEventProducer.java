package com.inventory_service.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String,Object> kafkaTemplate;

    public void sendInventoryReservedEvent(Object event){

        kafkaTemplate.send(
                "inventory-reserved-topic",
                event
        );
    }
}
