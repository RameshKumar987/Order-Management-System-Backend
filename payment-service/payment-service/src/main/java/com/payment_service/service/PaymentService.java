package com.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.dto.CreatePaymentRequest;
import com.payment_service.dto.PaymentResponse;
import com.payment_service.entity.Payment;
import com.payment_service.entity.PaymentStatus;
import com.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 1️⃣ Create Payment
    public PaymentResponse createPayment(CreatePaymentRequest request) {

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                "Payment initiated"
        );
    }

    // 2️⃣ Simulate Payment Success
    public PaymentResponse completePayment(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentId(UUID.randomUUID().toString());

        paymentRepository.save(payment);

        // Kafka Event
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId);
        event.put("status", "PAYMENT_SUCCESS");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(event);
            kafkaTemplate.send("payment-events", json);
        } catch (Exception e) {
            log.error("Error sending Kafka event: {}", e.getMessage());
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                "Payment successful"
        );
    }

    // 3️⃣ Fail Payment
    public PaymentResponse failPayment(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        // Kafka Event
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", orderId);
        event.put("status", "PAYMENT_FAILED");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(event);
            kafkaTemplate.send("payment-events", json);
        } catch (Exception e) {
            log.error("Error sending Kafka failure event: {}", e.getMessage());
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                "Payment failed"
        );
    }

    // 4️⃣ Get Payment
    public Payment getPayment(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    // 5️⃣ Auto-create payment
    public void autoCreatePayment(Long orderId, Double amount) {

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        System.out.println("🔥 Auto payment created for order: " + orderId);
    }
    // refundPayment
    public String refund(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getStatus().equals(PaymentStatus.SUCCESS)) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        return "Refund initiated";
    }
}
