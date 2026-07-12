package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> withdrawFunds(@RequestParam UUID vendorId, @RequestParam BigDecimal amount) {
        // In a real application, vendorId would be fetched from the authenticated user's context
        walletService.movePendingToWithdrawable(vendorId, amount);
        return ResponseEntity.ok("Withdrawal request initiated successfully");
    }
}
