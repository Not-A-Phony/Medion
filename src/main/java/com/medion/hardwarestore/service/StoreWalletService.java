package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.wallet.*;
import com.medion.hardwarestore.dto.payment.WalletBalanceResponse;
import com.medion.hardwarestore.dto.payment.WalletTransactionResponse;
import com.medion.hardwarestore.dto.payment.WithdrawalResponse;
import com.medion.hardwarestore.exception.WalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages store-keyed wallets ({@link StoreWallet}) - balances, withdrawals and audit trail.
 * Distinct from the vendor-keyed WalletService which handles order payouts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StoreWalletService {

    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository walletTransactionRepository;
    private final StoreRepository storeRepository;
    private final PaymentCacheService cacheService;

    private static final String BALANCE_CACHE_PREFIX = "wallet-balance::";

    public StoreWallet createWallet(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new WalletException("Store not found: " + storeId));

        StoreWallet wallet = StoreWallet.builder()
                .store(store)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .totalEarnings(BigDecimal.ZERO)
                .totalCommissionsPaid(BigDecimal.ZERO)
                .build();
        return storeWalletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getWalletBalance(UUID storeId) {
        StoreWallet wallet = storeWalletRepository.findByStoreId(storeId)
                .orElseThrow(() -> new WalletException("Wallet not found for store: " + storeId));

        BigDecimal total = wallet.getAvailableBalance().add(wallet.getLockedBalance());
        return new WalletBalanceResponse(
                storeId,
                wallet.getAvailableBalance(),
                wallet.getLockedBalance(),
                total,
                wallet.getTotalEarnings(),
                wallet.getTotalCommissionsPaid(),
                wallet.getLastWithdrawalDate());
    }

    public WithdrawalResponse withdrawFunds(UUID storeId, BigDecimal amount, String bankAccountNumber) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Withdrawal amount must be positive");
        }

        StoreWallet wallet = storeWalletRepository.findByStoreId(storeId)
                .orElseThrow(() -> new WalletException("Wallet not found for store: " + storeId));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new WalletException("Insufficient available balance");
        }

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        storeWalletRepository.save(wallet);

        StoreWalletTransaction txn = StoreWalletTransaction.builder()
                .storeWallet(wallet)
                .amount(amount)
                .transactionType(StoreWalletTransactionType.WITHDRAWAL)
                .referenceType("WITHDRAWAL")
                .description("Withdrawal to account " + maskAccount(bankAccountNumber))
                .status("PENDING")
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getAvailableBalance())
                .build();
        txn = walletTransactionRepository.save(txn);

        // Cache the withdrawal request context for 24h (settlement window).
        cacheService.put("withdrawal:" + txn.getId(), storeId.toString(), Duration.ofHours(24));
        invalidateWalletCache(storeId);

        log.info("Withdrawal {} of {} initiated for store {}", txn.getId(), amount, storeId);
        return new WithdrawalResponse(txn.getId(), "PENDING", amount, LocalDateTime.now().plusDays(1));
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionHistory(UUID storeId, Pageable pageable) {
        return walletTransactionRepository
                .findByStoreWalletStoreIdOrderByCreatedAtDesc(storeId, pageable)
                .map(this::mapToResponse);
    }

    public void invalidateWalletCache(UUID storeId) {
        cacheService.evict(BALANCE_CACHE_PREFIX + storeId);
    }

    private WalletTransactionResponse mapToResponse(StoreWalletTransaction tx) {
        return new WalletTransactionResponse(
                tx.getId(),
                tx.getAmount(),
                tx.getTransactionType().name(),
                tx.getDescription(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getCreatedAt());
    }

    private String maskAccount(String account) {
        if (account == null || account.length() <= 4) {
            return "****";
        }
        return "****" + account.substring(account.length() - 4);
    }
}
