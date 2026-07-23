package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.dto.payment.WalletBalanceResponse;
import com.medion.hardwarestore.dto.payment.WalletTransactionResponse;
import com.medion.hardwarestore.dto.payment.WithdrawalRequest;
import com.medion.hardwarestore.dto.payment.WithdrawalResponse;
import com.medion.hardwarestore.service.StoreWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Store-keyed wallet endpoints. Separate from the vendor {@code /api/v1/wallets}
 * controller which handles order payouts.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class StoreWalletController {

    private final StoreWalletService storeWalletService;

    @GetMapping("/{storeId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable UUID storeId) {
        return ResponseEntity.ok(storeWalletService.getWalletBalance(storeId));
    }

    @PostMapping("/{storeId}/withdraw")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @PathVariable UUID storeId,
            @Valid @RequestBody WithdrawalRequest request) {
        WithdrawalResponse response = storeWalletService.withdrawFunds(
                storeId, request.amount(), request.bankAccountNumber());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{storeId}/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(storeWalletService.getTransactionHistory(storeId, pageable));
    }
}
