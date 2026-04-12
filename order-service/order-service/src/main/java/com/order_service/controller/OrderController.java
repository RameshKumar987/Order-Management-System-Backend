package com.order_service.controller;

import com.order_service.dto.CreateOrderRequest;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderItemDto;
import com.order_service.repository.OrderRepository;
import com.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") Long userId
    ){
        log.info("📦 Order creation request: {}", request);
        log.info("Creating order for userId: {}", userId);

        try {
            var response = orderService.createOrder(request, userId);
            log.info("✅ Order created successfully: Order ID = {}", response.id());
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("❌ Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage(), "status", "VALIDATION_FAILED")
            );
        } catch (Exception e) {
            log.error("❌ Error creating order", e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Internal server error: " + e.getMessage(), "status", "ERROR")
            );
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> myOrders(@RequestHeader("X-User-Id") Long userId){

        log.info("myOrders request for userId: {}",userId);

        var orders = orderService.getUserOrders(userId);

        // Convert all Orders to OrderDtos to avoid circular reference
        var orderDtos = orders.stream()
                .map(order -> {
                    var itemDtos = order.getItems().stream()
                            .map(item -> new OrderItemDto(item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice()))
                            .toList();
                    return new OrderDto(
                            order.getId(),
                            order.getUserId(),
                            order.getStatus().name(),
                            itemDtos,
                            order.getDeliveryAddress(),
                            order.getDeliveryCity(),
                            order.getDeliveryLat(),
                            order.getDeliveryLng()
                    );
                })
                .toList();

        return ResponseEntity.ok(orderDtos);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllOrders(){
        var orders = orderRepository.findAll();

        // Convert all Orders to OrderDtos to avoid circular reference
        var orderDtos = orders.stream()
                .map(order -> {
                    var itemDtos = order.getItems().stream()
                            .map(item -> new OrderItemDto(item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice()))
                            .toList();
                    return new OrderDto(
                            order.getId(),
                            order.getUserId(),
                            order.getStatus().name(),
                            itemDtos,
                            order.getDeliveryAddress(),
                            order.getDeliveryCity(),
                            order.getDeliveryLat(),
                            order.getDeliveryLng()
                    );
                })
                .toList();

        return ResponseEntity.ok(orderDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id){
        var order = orderService.getOrderById(id);

        // Convert Entity to DTO to avoid circular reference in JSON serialization
        var itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice()))
                .toList();

        var orderDto = new OrderDto(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                itemDtos,
                order.getDeliveryAddress(),
                order.getDeliveryCity(),
                order.getDeliveryLat(),
                order.getDeliveryLng()
        );

        return ResponseEntity.ok(orderDto);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id){
        return ResponseEntity.ok(
                Map.of("message", orderService.cancelOrder(id))
        );
    }
}