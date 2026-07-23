package com.medion.hardwarestore.event;

import com.medion.hardwarestore.domain.payment.PaymentType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final UUID paymentTransactionId;
    private final UUID userId;
    private final BigDecimal amount;
    private final PaymentType paymentType;

    public PaymentCompletedEvent(Object source, UUID paymentTransactionId, UUID userId,
                                 BigDecimal amount, PaymentType paymentType) {
        super(source);
        this.paymentTransactionId = paymentTransactionId;
        this.userId = userId;
        this.amount = amount;
        this.paymentType = paymentType;
    }
}
