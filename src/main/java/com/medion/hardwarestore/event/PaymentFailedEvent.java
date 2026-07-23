package com.medion.hardwarestore.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PaymentFailedEvent extends ApplicationEvent {

    private final UUID paymentTransactionId;
    private final String failureReason;

    public PaymentFailedEvent(Object source, UUID paymentTransactionId, String failureReason) {
        super(source);
        this.paymentTransactionId = paymentTransactionId;
        this.failureReason = failureReason;
    }
}
