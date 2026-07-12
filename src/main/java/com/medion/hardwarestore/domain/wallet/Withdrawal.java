package com.medion.hardwarestore.domain.wallet;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "withdrawals")
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String provider; // MPESA, PAYPAL, STRIPE

    @Column(name = "account_details", nullable = false)
    private String accountDetails;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, FAILED

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
