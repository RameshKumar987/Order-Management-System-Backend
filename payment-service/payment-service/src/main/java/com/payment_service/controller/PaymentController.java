package com.payment_service.controller;

import com.payment_service.dto.CreatePaymentRequest;
import com.payment_service.dto.PaymentResponse;
import com.payment_service.repository.PaymentRepository;
import com.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    // Create Payment
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request) {
            //validate request
            if (request.orderId() == null || request.orderId() <= 0) {
                log.warn("❌ Invalid Order ID: {}", request.orderId());
                return ResponseEntity.badRequest().body(null);
            }
            if (request.amount() == null || request.amount() <= 0) {
                log.warn("❌ Invalid Amount: {}", request.amount());
                return ResponseEntity.badRequest().body(null);
            }

        return ResponseEntity.ok(paymentService.createPayment(request));
    }

    // Simulate success
    @PostMapping("/success/{orderId}")
    public ResponseEntity<PaymentResponse> success(@PathVariable Long orderId) {

        // ✅ Validate payment exists before processing
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("❌ Payment not found for Order ID: {}", orderId);
                    return new RuntimeException("Payment not found for Order ID: " + orderId);
                });

        log.info("✅ Processing payment success for Order ID: {}", orderId);
        return ResponseEntity.ok(paymentService.completePayment(orderId));
    }

    // Simulate failure
    @PostMapping("/fail/{orderId}")
    public ResponseEntity<PaymentResponse> fail(@PathVariable Long orderId) {

        // ✅ Validate payment exists before processing
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("❌ Payment not found for Order ID: {}", orderId);
                    return new RuntimeException("Payment not found for Order ID: " + orderId);
                });

        log.info("✅ Processing payment failure for Order ID: {}", orderId);
        return ResponseEntity.ok(paymentService.failPayment(orderId));
    }

    // Get payment
    @GetMapping("/{orderId}")
    public ResponseEntity<?> get(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPayment(orderId));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<?> refund(@PathVariable Long orderId) {

        // ✅ Validate payment exists before processing
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("❌ Payment not found for Order ID: {}", orderId);
                    return new RuntimeException("Payment not found for Order ID: " + orderId);
                });

        log.info("✅ Processing refund for Order ID: {}", orderId);
        return ResponseEntity.ok(paymentService.refund(orderId));
    }
}
