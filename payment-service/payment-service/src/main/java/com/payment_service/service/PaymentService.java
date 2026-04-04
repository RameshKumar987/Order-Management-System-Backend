package com.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.dto.ApiResponse;
import com.payment_service.dto.CreatePaymentRequest;
import com.payment_service.entity.Payment;
import com.payment_service.entity.PaymentStatus;
import com.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ✅ CREATE PAYMENT
    public ApiResponse createPayment(CreatePaymentRequest request) {

        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new RuntimeException("Payment already exists for this order");
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("✅ Payment created for order: {}", request.orderId());

        return ApiResponse.builder()
                .success(true)
                .message("Payment initiated successfully")
                .data(payment)
                .build();
    }

    // ✅ SUCCESS
    public ApiResponse completePayment(Long orderId) {

        Payment payment = getPaymentEntity(orderId);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return ApiResponse.builder()
                    .success(true)
                    .message("Payment already completed")
                    .data(payment)
                    .build();
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentId(UUID.randomUUID().toString());

        paymentRepository.save(payment);

        sendKafkaEvent(orderId, "PAYMENT_SUCCESS");

        log.info("💰 Payment SUCCESS for order: {}", orderId);

        return ApiResponse.builder()
                .success(true)
                .message("Payment completed successfully")
                .data(payment)
                .build();
    }

    // ❌ FAILURE
    public ApiResponse failPayment(Long orderId) {

        Payment payment = getPaymentEntity(orderId);

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return ApiResponse.builder()
                    .success(false)
                    .message("Payment already failed")
                    .data(payment)
                    .build();
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        sendKafkaEvent(orderId, "PAYMENT_FAILED");

        log.warn("❌ Payment FAILED for order: {}", orderId);

        return ApiResponse.builder()
                .success(false)
                .message("Payment failed")
                .data(payment)
                .build();
    }

    // 🔍 GET PAYMENT
    public ApiResponse getPayment(Long orderId) {

        Payment payment = getPaymentEntity(orderId);

        return ApiResponse.builder()
                .success(true)
                .message("Payment fetched successfully")
                .data(payment)
                .build();
    }

    // 📜 HISTORY
    public ApiResponse getAllPayments() {

        return ApiResponse.builder()
                .success(true)
                .message("All payments fetched")
                .data(paymentRepository.findAll())
                .build();
    }

    // 💸 REFUND
    public ApiResponse refund(Long orderId) {

        Payment payment = getPaymentEntity(orderId);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        log.info("💸 Refund processed for order: {}", orderId);

        return ApiResponse.builder()
                .success(true)
                .message("Refund initiated successfully")
                .data(payment)
                .build();
    }

    // 🔁 AUTO CREATE (Inventory → Payment)
    public void autoCreatePayment(Long orderId, Double amount) {

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            log.warn("⚠️ Payment already exists for order: {}", orderId);
            return;
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("🔥 Auto payment created for order: {}", orderId);
    }

    // 🔥 PRIVATE METHOD
    private Payment getPaymentEntity(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    // 🔥 KAFKA EVENT
    private void sendKafkaEvent(Long orderId, String status) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("orderId", orderId);
            event.put("status", status);

            String json = objectMapper.writeValueAsString(event);

            kafkaTemplate.send("payment-events", json);

        } catch (Exception e) {
            log.error("Kafka error: {}", e.getMessage());
        }
    }
}