package com.order_service.service;

import com.order_service.dto.CreateOrderRequest;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderItemDto;
import com.order_service.dto.OrderResponse;
import com.order_service.entity.Order;
import com.order_service.entity.OrderItem;
import com.order_service.entity.OrderStatusHistory;
import com.order_service.enums.OrderStatus;
import com.order_service.kafka.OrderProducer;
import com.order_service.repository.OrderHistoryRepository;
import com.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final OrderHistoryRepository historyRepository;


    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(0.0);
        order.setDeliveryAddress(request.deliveryAddress());
        order.setDeliveryCity(request.deliveryCity());
        order.setDeliveryLat(request.deliveryLat());
        order.setDeliveryLng(request.deliveryLng());

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty");
        }

        // Validate all items have quantity > 0
        request.items().forEach(item -> {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be greater than 0 for product: " + item.productId());
            }
        });

        List<OrderItem> items = request.items().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.productId())
                        .productName(i.productName())
                        .quantity(i.quantity())
                        .price(0.0)
                        .order(order)
                        .build())
                .toList();

        order.setItems(items);

        Order savedOrder = orderRepository.save(order);

        saveHistory(savedOrder);

        // Publish order event for inventory validation
        List<OrderItemDto> itemDtos = savedOrder.getItems().stream()
                .map(item -> new OrderItemDto(item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice()))
                .toList();
        OrderDto orderDto = new OrderDto(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getStatus().name(),
                itemDtos,
                savedOrder.getDeliveryAddress(),
                savedOrder.getDeliveryCity(),
                savedOrder.getDeliveryLat(),
                savedOrder.getDeliveryLng()
        );
        orderProducer.publishOrderCreated(orderDto);

        // Return complete response with all details
        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getStatus().name(),
                itemDtos,
                savedOrder.getTotalAmount(),
                savedOrder.getCreatedAt(),
                savedOrder.getDeliveryAddress(),
                savedOrder.getDeliveryCity(),
                savedOrder.getDeliveryLat(),
                savedOrder.getDeliveryLng()
        );
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        try {
            Order order = getOrderById(orderId);

            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
                log.warn("⚠️  Cannot confirm order with status: {}", order.getStatus());
                return;
            }

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            saveHistory(order);

            log.info("✅ Order confirmed successfully: Order ID = {}", orderId);
        } catch (Exception e) {
            log.error("❌ Error confirming order: {}", e.getMessage());
            throw new RuntimeException("Failed to confirm order: " + e.getMessage());
        }
    }

    public List<Order> getUserOrders(Long userId){
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long id){
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public String cancelOrder(Long id){

        Order order = getOrderById(id);

        if(order.getStatus() == OrderStatus.CONFIRMED){
            throw new RuntimeException("Cannot cancel confirmed order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        saveHistory(saved);

        // Notify inventory to release stock
        List<OrderItemDto> itemDtos = saved.getItems().stream()
                .map(item -> new OrderItemDto(item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice()))
                .toList();
        OrderDto orderDto = new OrderDto(saved.getId(), saved.getUserId(), saved.getStatus().name(), itemDtos,
                saved.getDeliveryAddress(), saved.getDeliveryCity(), saved.getDeliveryLat(), saved.getDeliveryLng());
        orderProducer.publishOrderCancelled(orderDto);

        return "Order cancelled successfully";
    }

    public void saveHistory(Order order){

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(order.getId());
        history.setStatus(order.getStatus().name());
        history.setChangedAt(LocalDateTime.now());

        historyRepository.save(history);
    }
}