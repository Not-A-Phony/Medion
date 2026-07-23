package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.subscription.*;
import com.medion.hardwarestore.dto.payment.SubscriptionPlan;
import com.medion.hardwarestore.dto.payment.SubscriptionStatusResponse;
import com.medion.hardwarestore.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;

    /**
     * The two supported subscription plans. Static for now; cache invalidation
     * would apply if these become data-driven.
     */
    @Transactional(readOnly = true)
    public List<SubscriptionPlan> getAvailablePlans() {
        SubscriptionPlan professional = new SubscriptionPlan(
                SubscriptionPlanType.MONTHLY_FLAT_RATE.name(),
                "Professional Plan",
                "Flat monthly fee with 100% sales retention - no commission on sales.",
                new BigDecimal("9999"),
                null,
                List.of("100% sales retention", "Unlimited products", "Priority support"));

        SubscriptionPlan starter = new SubscriptionPlan(
                SubscriptionPlanType.COMMISSION_BASED.name(),
                "Starter Plan",
                "No monthly fee - a 5% commission is taken on each sale.",
                null,
                new BigDecimal("5.0"),
                List.of("No monthly fee", "5% commission per sale", "Standard support"));

        return List.of(professional, starter);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(UUID storeId) {
        return subscriptionRepository.findByStoreId(storeId)
                .map(s -> new SubscriptionStatusResponse(
                        true,
                        s.getStatus() != null ? s.getStatus().name() : null,
                        s.getSubscriptionPlanType() != null ? s.getSubscriptionPlanType().name() : null,
                        s.getCurrentBillingCycleStart(),
                        s.getCurrentBillingCycleEnd(),
                        s.getNextPaymentDate(),
                        s.getLastPaymentDate(),
                        s.getAutoRenewal(),
                        s.getMonthlyFlatRate(),
                        s.getCommissionPercentage()))
                .orElseGet(SubscriptionStatusResponse::none);
    }

    public void cancelSubscription(UUID storeId, String reason) {
        Subscription subscription = subscriptionRepository.findByStoreId(storeId)
                .orElseThrow(() -> new PaymentException("Subscription not found for store: " + storeId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancellationReason(reason);
        subscription.setAutoRenewal(false);
        subscriptionRepository.save(subscription);
        log.info("Subscription cancelled for store {} (reason: {})", storeId, reason);
    }

    /**
     * Daily job (01:00) that issues renewal invoices for due auto-renewing subscriptions.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void processSubscriptionRenewals() {
        List<Subscription> due = subscriptionRepository
                .findByAutoRenewalAndNextPaymentDateBefore(true, LocalDateTime.now());
        log.info("Processing {} subscription renewal(s)", due.size());

        for (Subscription subscription : due) {
            try {
                processSubscriptionRenewal(subscription);
            } catch (Exception e) {
                log.error("Failed to renew subscription {}", subscription.getId(), e);
            }
        }
    }

    private void processSubscriptionRenewal(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = calculateRenewalAmount(subscription);

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .subscription(subscription)
                .invoiceNumber(generateInvoiceNumber(subscription.getId()))
                .billingCycleStart(now)
                .billingCycleEnd(now.plusMonths(1))
                .amountCharged(amount)
                .dueDate(now.plusDays(3))
                .status(SubscriptionPaymentStatus.PENDING)
                .build();
        subscriptionPaymentRepository.save(payment);
        log.info("Renewal invoice {} created for subscription {}",
                payment.getInvoiceNumber(), subscription.getId());
    }

    private BigDecimal calculateRenewalAmount(Subscription subscription) {
        if (subscription.getSubscriptionPlanType() == SubscriptionPlanType.MONTHLY_FLAT_RATE) {
            return subscription.getMonthlyFlatRate() != null
                    ? subscription.getMonthlyFlatRate() : BigDecimal.ZERO;
        }
        return calculateCommissionDue(subscription);
    }

    private BigDecimal calculateCommissionDue(Subscription subscription) {
        // TODO: compute from the store's sales for the billing cycle.
        return BigDecimal.ZERO;
    }

    private String generateInvoiceNumber(UUID subscriptionId) {
        return "INV-SUB-" + subscriptionId + "-" + System.currentTimeMillis();
    }
}
