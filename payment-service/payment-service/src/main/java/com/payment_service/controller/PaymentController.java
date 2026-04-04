package com.payment_service.controller;

import com.payment_service.dto.ApiResponse;
import com.payment_service.dto.CreatePaymentRequest;
import com.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

        return ResponseEntity.ok(paymentService.completePayment(orderId, null));
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
    public ResponseEntity<ApiResponse> getAllPayments(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("📜 Fetching payment history for userId: {}", userId);
        return ResponseEntity.ok(paymentService.getAllPayments(userId));
    }


    // 💸 REFUND
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponse> refund(@PathVariable Long orderId) {

        log.info("💸 Refund API hit for order: {}", orderId);

        return ResponseEntity.ok(paymentService.refund(orderId));
    }

    // 💳 RAZORPAY
    @PostMapping("/razorpay/create-order/{orderId}")
    public ResponseEntity<ApiResponse> createRzpOrder(@PathVariable Long orderId) {
        log.info("💳 Creating Razorpay order for OMS order: {}", orderId);
        return ResponseEntity.ok(paymentService.createRazorpayOrder(orderId));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<ApiResponse> verifyRzpOrder(@RequestBody Map<String, String> data) {
        log.info("💳 Verifying Razorpay payment: {}", data);
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(data));
    }
}