package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.payment.*;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.subscription.*;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.wallet.*;
import com.medion.hardwarestore.dto.payment.MpesaCallbackRequest;
import com.medion.hardwarestore.dto.payment.PaymentResponse;
import com.medion.hardwarestore.event.PaymentCompletedEvent;
import com.medion.hardwarestore.event.PaymentFailedEvent;
import com.medion.hardwarestore.exception.MpesaException;
import com.medion.hardwarestore.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository walletTransactionRepository;
    private final StoreRepository storeRepository;
    private final MpesaStkService mpesaStkService;
    private final PaymentCacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SUBSCRIPTION_CTX_PREFIX = "payment:subscription:";
    private static final BigDecimal DEFAULT_COMMISSION_PERCENTAGE = new BigDecimal("5.0");

    /**
     * Initiate a subscription payment via M-Pesa STK Push.
     */
    public PaymentResponse initiateSubscriptionPayment(User user, BigDecimal amount,
                                                       String phoneNumber, UUID storeId) {
        if (user == null) {
            throw new PaymentException("Authenticated user is required");
        }
        if (storeId == null) {
            throw new PaymentException("storeId is required");
        }

        subscriptionRepository.findByStoreId(storeId).ifPresent(existing -> {
            if (existing.getStatus() == SubscriptionStatus.ACTIVE) {
                throw new PaymentException("Store already has an active subscription");
            }
        });

        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .storeId(storeId)
                .amount(amount)
                .status(PaymentTransactionStatus.PENDING)
                .paymentType(PaymentType.SUBSCRIPTION)
                .phoneNumber(phoneNumber)
                .externalReferenceId(generateExternalReference(storeId))
                .build();
        transaction = paymentTransactionRepository.save(transaction);

        try {
            String checkoutRequestId = mpesaStkService.initiateStkPush(
                    phoneNumber, amount, generateAccountReference(storeId), transaction.getId());

            // Cache store context against the checkout id so the callback can resolve it.
            cacheService.put(SUBSCRIPTION_CTX_PREFIX + checkoutRequestId,
                    storeId.toString(), Duration.ofMinutes(10));

            transaction.setStatus(PaymentTransactionStatus.PROCESSING);
            paymentTransactionRepository.save(transaction);

            return new PaymentResponse(
                    transaction.getId(),
                    checkoutRequestId,
                    transaction.getStatus().name(),
                    "STK Push sent. Enter your M-Pesa PIN to complete payment.",
                    amount,
                    null,
                    transaction.getCreatedAt());
        } catch (MpesaException e) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            paymentTransactionRepository.save(transaction);
            throw new PaymentException("Failed to initiate subscription payment", e);
        }
    }

    /**
     * Handle an M-Pesa callback: complete or fail the transaction and trigger side-effects.
     */
    public void handleMpesaCallback(MpesaCallbackRequest callbackRequest) {
        String checkoutRequestId = callbackRequest.checkoutRequestID();
        Integer resultCode = callbackRequest.resultCode();
        String resultDesc = callbackRequest.resultDesc();

        Optional<UUID> paymentIdOpt = mpesaStkService.resolveTransactionId(checkoutRequestId);
        if (paymentIdOpt.isEmpty()) {
            log.error("No cached payment transaction for checkout {}", checkoutRequestId);
            return;
        }

        PaymentTransaction transaction = paymentTransactionRepository.findById(paymentIdOpt.get())
                .orElse(null);
        if (transaction == null) {
            log.error("Payment transaction {} not found for checkout {}",
                    paymentIdOpt.get(), checkoutRequestId);
            return;
        }

        if (resultCode != null && resultCode == 0) {
            transaction.setStatus(PaymentTransactionStatus.COMPLETED);
            transaction.setMpesaReceiptNumber(callbackRequest.mpesaReceiptNumber());
            transaction.setMpesaTransactionId(callbackRequest.transactionId());
            transaction.setCompletedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            if (transaction.getPaymentType() == PaymentType.SUBSCRIPTION) {
                processSubscriptionActivation(transaction, checkoutRequestId);
            }

            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    this, transaction.getId(),
                    transaction.getUser() != null ? transaction.getUser().getId() : null,
                    transaction.getAmount(), transaction.getPaymentType()));
            log.info("Payment {} completed via checkout {}", transaction.getId(), checkoutRequestId);
        } else {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setErrorMessage(resultDesc);
            paymentTransactionRepository.save(transaction);

            eventPublisher.publishEvent(new PaymentFailedEvent(
                    this, transaction.getId(), resultDesc));
            log.warn("Payment {} failed via checkout {}: {}",
                    transaction.getId(), checkoutRequestId, resultDesc);
        }
    }

    /**
     * Activate (or refresh) a store subscription after a successful subscription payment.
     */
    public void processSubscriptionActivation(PaymentTransaction transaction, String checkoutRequestId) {
        UUID storeId = cacheService.get(SUBSCRIPTION_CTX_PREFIX + checkoutRequestId)
                .map(UUID::fromString)
                .orElse(transaction.getStoreId());
        if (storeId == null) {
            log.error("Cannot activate subscription: no store context for checkout {}", checkoutRequestId);
            return;
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new PaymentException("Store not found: " + storeId));

        Subscription subscription = subscriptionRepository.findByStoreId(storeId)
                .orElseGet(() -> Subscription.builder()
                        .store(store)
                        .subscriptionPlanType(SubscriptionPlanType.MONTHLY_FLAT_RATE)
                        .build());

        LocalDateTime now = LocalDateTime.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentBillingCycleStart(now);
        subscription.setCurrentBillingCycleEnd(now.plusMonths(1));
        subscription.setNextPaymentDate(now.plusMonths(1));
        subscription.setLastPaymentDate(now);

        if (subscription.getSubscriptionPlanType() == SubscriptionPlanType.MONTHLY_FLAT_RATE) {
            subscription.setMonthlyFlatRate(transaction.getAmount());
        } else {
            subscription.setCommissionPercentage(DEFAULT_COMMISSION_PERCENTAGE);
        }

        subscriptionRepository.save(subscription);
        log.info("Subscription activated for store {}", storeId);
    }

    /**
     * Credit a store wallet from marketplace earnings, applying commission for
     * commission-based subscriptions and recording an audit trail.
     */
    public void creditStoreWallet(UUID storeId, BigDecimal amount, String referenceType, UUID referenceId) {
        StoreWallet wallet = storeWalletRepository.findByStoreId(storeId)
                .orElseThrow(() -> new PaymentException("Store wallet not found: " + storeId));

        Subscription subscription = subscriptionRepository.findByStoreId(storeId).orElse(null);

        BigDecimal commissionAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = amount;
        if (subscription != null
                && subscription.getSubscriptionPlanType() == SubscriptionPlanType.COMMISSION_BASED
                && subscription.getCommissionPercentage() != null) {
            commissionAmount = amount
                    .multiply(subscription.getCommissionPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            creditAmount = amount.subtract(commissionAmount);
        }

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(creditAmount));
        wallet.setTotalEarnings(wallet.getTotalEarnings().add(amount));
        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setTotalCommissionsPaid(wallet.getTotalCommissionsPaid().add(commissionAmount));
        }
        storeWalletRepository.save(wallet);

        String refId = referenceId != null ? referenceId.toString() : null;
        StoreWalletTransaction creditTxn = StoreWalletTransaction.builder()
                .storeWallet(wallet)
                .amount(creditAmount)
                .transactionType(StoreWalletTransactionType.CREDIT)
                .referenceId(refId)
                .referenceType(referenceType)
                .description("Earnings credit")
                .status("COMPLETED")
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getAvailableBalance())
                .build();
        walletTransactionRepository.save(creditTxn);

        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            StoreWalletTransaction commissionTxn = StoreWalletTransaction.builder()
                    .storeWallet(wallet)
                    .amount(commissionAmount)
                    .transactionType(StoreWalletTransactionType.COMMISSION_DEDUCTION)
                    .referenceId(refId)
                    .referenceType(referenceType)
                    .description("Platform commission deduction")
                    .status("COMPLETED")
                    .balanceBefore(wallet.getAvailableBalance())
                    .balanceAfter(wallet.getAvailableBalance())
                    .build();
            walletTransactionRepository.save(commissionTxn);
        }

        log.info("Credited store {} wallet with {} (commission {})", storeId, creditAmount, commissionAmount);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException("Payment transaction not found: " + transactionId));

        return new PaymentResponse(
                transaction.getId(),
                null,
                transaction.getStatus().name(),
                null,
                transaction.getAmount(),
                transaction.getMpesaReceiptNumber(),
                transaction.getCreatedAt());
    }

    private String generateExternalReference(UUID storeId) {
        return "SUB-" + storeId + "-" + System.currentTimeMillis();
    }

    private String generateAccountReference(UUID storeId) {
        return "STORE" + storeId.toString().replace("-", "").substring(0, 8);
    }
}
