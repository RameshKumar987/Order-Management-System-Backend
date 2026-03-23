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
import java.util.Map;

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
                .map(item -> new OrderItemDto(item.getProductId(), item.getQuantity(), item.getPrice()))
                .toList();
        OrderDto orderDto = new OrderDto(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getStatus().name(),
                itemDtos
        );
        orderProducer.publishOrderCreated(orderDto);

        // Return complete response with all details
        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getStatus().name(),
                itemDtos,
                savedOrder.getTotalAmount(),
                savedOrder.getCreatedAt()
        );
    }

    @Transactional
    public void updateOrderFromValidation(Map<String, Object> validationResult) {
        try {
            Long orderId = ((Number) validationResult.get("orderId")).longValue();
            Order order = getOrderById(orderId);

            String status = (String) validationResult.get("status");

            if ("VALIDATED".equals(status)) {
                // ✅ Validation successful: Order is approved
                Double totalAmount = (Double) validationResult.get("totalAmount");
                if (totalAmount == null || totalAmount < 0) {
                    throw new IllegalArgumentException("Invalid total amount from validation: " + totalAmount);
                }
                order.setTotalAmount(totalAmount);
                order.setStatus(OrderStatus.CONFIRMED);

                // Update item prices with calculated total
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    double pricePerItem = totalAmount / order.getItems().size();
                    order.getItems().forEach(item -> item.setPrice(pricePerItem));
                    log.info("✅ Item prices updated for Order ID: {}, Price per item: {}", orderId, pricePerItem);
                }

                orderRepository.save(order);
                saveHistory(order);
                log.info("✅ Order confirmed and saved: Order ID = {}, Total Amount = {}", orderId, totalAmount);

            } else if ("FAILED".equals(status)) {
                // ❌ Validation failed: Order is rejected
                String errorMsg = (String) validationResult.get("error");
                order.setStatus(OrderStatus.FAILED);

                orderRepository.save(order);
                saveHistory(order);

                log.warn("⚠️  Order validation FAILED for Order ID: {}, Reason: {}", orderId, errorMsg);
                log.warn("⚠️  Order status updated to FAILED: Order ID = {}", orderId);

            } else {
                log.error("❌ Unknown validation status: {}", status);
                throw new IllegalArgumentException("Unknown validation status: " + status);
            }
        } catch (ClassCastException | NullPointerException e) {
            log.error("❌ Type conversion error in validation result", e);
            throw new RuntimeException("Invalid validation result format: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error updating order from validation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update order from validation: " + e.getMessage(), e);
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
        orderRepository.save(order);

        saveHistory(order);

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