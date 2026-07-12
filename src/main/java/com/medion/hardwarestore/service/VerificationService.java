package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.store.Verification;
import com.medion.hardwarestore.domain.store.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;

    public Verification submitVerificationDocuments(UUID storeId, String documentUrls) {
        Verification verification = verificationRepository.findByStoreId(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found"));
        
        verification.setDocumentUrls(documentUrls);
        verification.setStatus("PENDING");
        return verificationRepository.save(verification);
    }

    public Verification updateOtpStatus(UUID storeId, boolean verified) {
        Verification verification = verificationRepository.findByStoreId(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found"));
        
        verification.setOtpVerified(verified);
        return verificationRepository.save(verification);
    }
}
