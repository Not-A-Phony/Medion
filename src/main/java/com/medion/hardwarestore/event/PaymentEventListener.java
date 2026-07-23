package com.medion.hardwarestore.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to payment lifecycle events. Notification delivery (email) is left as a
 * logged hook until a dedicated EmailService is wired in, so this compiles and runs
 * without a hard dependency on mail infrastructure.
 */
@Slf4j
@Component
public class PaymentEventListener {

    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed: transaction={}, user={}, amount={}, type={}",
                event.getPaymentTransactionId(), event.getUserId(), event.getAmount(), event.getPaymentType());
        // TODO: emailService.sendPaymentConfirmationEmail(event) once EmailService exists.
    }

    @EventListener
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed: transaction={}, reason={}",
                event.getPaymentTransactionId(), event.getFailureReason());
        // TODO: emailService.sendPaymentFailureEmail(event) once EmailService exists.
    }
}
