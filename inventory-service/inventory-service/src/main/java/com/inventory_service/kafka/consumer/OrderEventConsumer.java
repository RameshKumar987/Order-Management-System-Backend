package com.inventory_service.kafka.consumer;

import com.inventory_service.dto.OrderEvent;
import com.inventory_service.dto.ReserveStockRequest;
import com.inventory_service.kafka.producer.InventoryEventProducer;
import com.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * DEPRECATED: This consumer is disabled in favor of OrderConsumer
 * which properly handles multiple items per order.
 *
 * Reason: OrderEventConsumer only supported single product per message
 * but orders can have multiple items. Use OrderConsumer instead.
 */
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    /**
     * DISABLED: This method is not active as it's replaced by OrderConsumer.consumeOrderCreated()
     *
     * The issue with this consumer:
     * 1. It only processes single product (OrderEvent has no items list)
     * 2. Orders can have multiple items, so this would lose items
     * 3. No stock check before reservation
     * 4. No validation result published back
     *
     * Use OrderConsumer instead which:
     * 1. Processes OrderDto with multiple items
     * 2. Validates stock for ALL items
     * 3. Reserves stock for all items
     * 4. Publishes validation result to order-validation-topic
     */
    // @KafkaListener(
    //         topics = "order-events",
    //         groupId = "inventory-group",
    //         containerFactory = "kafkaListenerContainerFactory"
    // )
    public void consume(OrderEvent orderEvent){
        if ("ORDER_CREATED".equals(orderEvent.getEventType())) {
            try {
                inventoryService.reserveStock(new ReserveStockRequest(orderEvent.getOrderId(), orderEvent.getProductId(), orderEvent.getQuantity()));
                inventoryEventProducer.sendInventoryReservedEvent(orderEvent);
            } catch (Exception e) {
                inventoryEventProducer.sendInventoryFailedEvent(orderEvent);
            }
        }
    }
}