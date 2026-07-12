package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.domain.store.Verification;
import com.medion.hardwarestore.service.OtpService;
import com.medion.hardwarestore.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final OtpService otpService;

    @PostMapping("/submit-documents")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Verification> submitDocuments(@RequestParam UUID storeId, @RequestParam String documentUrls) {
        return ResponseEntity.ok(verificationService.submitVerificationDocuments(storeId, documentUrls));
    }

    @PostMapping("/send-otp")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        otpService.generateAndSendOtp(email);
        return ResponseEntity.ok("OTP sent successfully to " + email);
    }

    @PostMapping("/verify-otp")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<String> verifyOtp(@RequestParam UUID storeId, @RequestParam String email, @RequestParam String otp) {
        boolean isValid = otpService.verifyOtp(email, otp);
        if (isValid) {
            verificationService.updateOtpStatus(storeId, true);
            return ResponseEntity.ok("OTP verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }
    }
}
