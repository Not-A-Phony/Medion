package com.medion.hardwarestore.domain.subscription;

import com.medion.hardwarestore.domain.store.Store;
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
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", unique = true, nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan_type", nullable = false)
    private SubscriptionPlanType subscriptionPlanType;

    @Column(name = "monthly_flat_rate")
    private BigDecimal monthlyFlatRate;

    @Column(name = "commission_percentage")
    private BigDecimal commissionPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "current_billing_cycle_start")
    private LocalDateTime currentBillingCycleStart;

    @Column(name = "current_billing_cycle_end")
    private LocalDateTime currentBillingCycleEnd;

    @Column(name = "auto_renewal")
    @Builder.Default
    private Boolean autoRenewal = true;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "next_payment_date")
    private LocalDateTime nextPaymentDate;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
