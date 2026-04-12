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

import com.payment_service.dto.RazorpayOrderResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key}")
    private String razorpayKeyId;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    // ✅ CREATE PAYMENT
    public ApiResponse createPayment(CreatePaymentRequest request) {

        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new RuntimeException("Payment already exists for this order");
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("✅ Payment created for order: {} by user: {}", request.orderId(), request.userId());

        return ApiResponse.builder()
                .success(true)
                .message("Payment initiated successfully")
                .data(payment)
                .build();
    }

    // ✅ SUCCESS
    public ApiResponse completePayment(Long orderId, String rzpPaymentId) {

        Payment payment = getPaymentEntity(orderId);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return ApiResponse.builder()
                    .success(true)
                    .message("Payment already completed")
                    .data(payment)
                    .build();
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentId(rzpPaymentId != null ? rzpPaymentId : UUID.randomUUID().toString());

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
    public ApiResponse getAllPayments(Long userId) {

        return ApiResponse.builder()
                .success(true)
                .message("User payments fetched")
                .data(paymentRepository.findByUserId(userId))
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
    public void autoCreatePayment(Long orderId, Long userId, Double amount) {

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            log.warn("⚠️ Payment already exists for order: {}", orderId);
            return;
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("🔥 Auto payment created for order: {} for user: {}", orderId, userId);
    }

    // 💳 RAZORPAY ORDER CREATE
    public ApiResponse createRazorpayOrder(Long orderId) {
        try {
            Payment payment = getPaymentEntity(orderId);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (payment.getAmount() * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + orderId);

            Order rzpOrder = razorpayClient.orders.create(orderRequest);

            // Temporarily store Razorpay Order ID as paymentId
            payment.setPaymentId(rzpOrder.get("id")); 
            paymentRepository.save(payment);

            RazorpayOrderResponse response = RazorpayOrderResponse.builder()
                    .rzpOrderId(rzpOrder.get("id"))
                    .amount(payment.getAmount())
                    .currency("INR")
                    .keyId(razorpayKeyId)
                    .orderId(orderId)
                    .build();

            return ApiResponse.builder()
                    .success(true)
                    .message("Razorpay order created")
                    .data(response)
                    .build();

        } catch (Exception e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Razorpay Order Creation Failed: " + e.getMessage());
        }
    }

    // 💳 RAZORPAY VERIFY
    public ApiResponse verifyRazorpayPayment(Map<String, String> data) {
        try {
            String rzpOrderId = data.get("razorpay_order_id");
            String rzpPaymentId = data.get("razorpay_payment_id");
            String rzpSignature = data.get("razorpay_signature");
            Long orderId = Long.valueOf(data.get("orderId"));

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", rzpOrderId);
            options.put("razorpay_payment_id", rzpPaymentId);
            options.put("razorpay_signature", rzpSignature);

            boolean isValid = Utils.verifyPaymentSignature(options, razorpaySecret);
            
            if (isValid) {
                return completePayment(orderId, rzpPaymentId);
            } else {
                log.warn("❌ Razorpay signature invalid for order: {}", orderId);
                return failPayment(orderId);
            }

        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return failPayment(Long.valueOf(data.get("orderId")));
        }
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