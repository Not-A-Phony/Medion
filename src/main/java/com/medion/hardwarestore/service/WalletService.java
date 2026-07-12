package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.wallet.Transaction;
import com.medion.hardwarestore.domain.wallet.TransactionRepository;
import com.medion.hardwarestore.domain.wallet.Wallet;
import com.medion.hardwarestore.domain.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void creditPendingBalance(UUID vendorId, BigDecimal amount, String referenceId) {
        Wallet wallet = walletRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for vendor"));

        wallet.setPendingBalance(wallet.getPendingBalance().add(amount));
        walletRepository.save(wallet);

        createTransaction(wallet, "SALE_PENDING", amount, referenceId);
    }

    @Transactional
    public void creditWithdrawableBalance(UUID vendorId, BigDecimal amount, String referenceId) {
        Wallet wallet = walletRepository.findByVendorId(vendorId)
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder().vendor(com.medion.hardwarestore.domain.user.User.builder().id(vendorId).build()).build();
                    return walletRepository.save(newWallet);
                });

        wallet.setWithdrawableBalance(wallet.getWithdrawableBalance().add(amount));
        wallet.setBalance(wallet.getPendingBalance().add(wallet.getWithdrawableBalance()));
        walletRepository.save(wallet);

        createTransaction(wallet, "SALE_COMPLETED", amount, referenceId);
    }

    @Transactional
    public void movePendingToWithdrawable(UUID vendorId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for vendor"));

        if (wallet.getPendingBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient pending balance");
        }

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));
        wallet.setWithdrawableBalance(wallet.getWithdrawableBalance().add(amount));
        wallet.setBalance(wallet.getPendingBalance().add(wallet.getWithdrawableBalance()));
        walletRepository.save(wallet);
    }

    private void createTransaction(Wallet wallet, String type, BigDecimal amount, String referenceId) {
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .referenceId(referenceId)
                .status("COMPLETED")
                .build();
        transactionRepository.save(transaction);
    }
}
