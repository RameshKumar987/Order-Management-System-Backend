package com.inventory_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder
                .name("order-created-topic")
                .build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder
                .name("inventory-reserved-topic")
                .build();
    }

}
