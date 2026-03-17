package com.inventory_service.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    @KafkaListener(
            topics = "order-created-topic",
            groupId = "inventory-group"
    )
    public void consume(String message){

        System.out.println(
                "Order received: " + message
        );
    }
}
