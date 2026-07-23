package com.medion.hardwarestore.domain.subscription;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_payments")
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @Column(name = "billing_cycle_start", nullable = false)
    private LocalDateTime billingCycleStart;

    @Column(name = "billing_cycle_end", nullable = false)
    private LocalDateTime billingCycleEnd;

    @Column(name = "amount_charged", nullable = false)
    private BigDecimal amountCharged;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPaymentStatus status;

    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
