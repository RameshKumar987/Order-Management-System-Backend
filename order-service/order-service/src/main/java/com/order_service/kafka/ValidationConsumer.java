package com.order_service.kafka;

/**
 * ⚠️ DEPRECATED - This class has been moved to PaymentEventConsumer.java
 *
 * This file is kept for reference only and should be deleted.
 * The PaymentEventConsumer class now handles payment event processing for the Saga pattern.
 *
 * MIGRATION DETAILS:
 * - Old class: ValidationConsumer (listened to old validation topic)
 * - New class: PaymentEventConsumer (listens to payment-events topic)
 * - Location: PaymentEventConsumer.java (same package)
 *
 * Spring will automatically pick up the PaymentEventConsumer bean.
 * This file can be safely deleted.
 */
@Deprecated(since = "1.0", forRemoval = true)
public class ValidationConsumer {
    // This class is deprecated and has been replaced by PaymentEventConsumer.java
}

