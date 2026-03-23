package com.order_service.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent {

    private String eventType;
    private Long orderId;
}
