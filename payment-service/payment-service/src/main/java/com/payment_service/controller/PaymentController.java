package com.payment_service.controller;

import com.payment_service.dto.ApiResponse;
import com.payment_service.dto.CreatePaymentRequest;
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

    // ✅ CREATE
    @PostMapping("/create")
    public ResponseEntity<ApiResponse> create(
            @Valid @RequestBody CreatePaymentRequest request) {

        log.info("📥 Create payment request: {}", request);

        return ResponseEntity.ok(paymentService.createPayment(request));
    }

    // ✅ SUCCESS
    @PostMapping("/success/{orderId}")
    public ResponseEntity<ApiResponse> success(@PathVariable Long orderId) {

        log.info("💰 Payment success API hit for order: {}", orderId);

        return ResponseEntity.ok(paymentService.completePayment(orderId));
    }

    // ❌ FAIL
    @PostMapping("/fail/{orderId}")
    public ResponseEntity<ApiResponse> fail(@PathVariable Long orderId) {

        log.info("❌ Payment fail API hit for order: {}", orderId);

        return ResponseEntity.ok(paymentService.failPayment(orderId));
    }

    // 🔍 GET
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse> get(@PathVariable Long orderId) {

        return ResponseEntity.ok(paymentService.getPayment(orderId));
    }

    // 📜 HISTORY
    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getAllPayments() {

        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // 💸 REFUND
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponse> refund(@PathVariable Long orderId) {

        log.info("💸 Refund API hit for order: {}", orderId);

        return ResponseEntity.ok(paymentService.refund(orderId));
    }
}