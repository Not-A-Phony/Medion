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
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false)
    private String type; // SALE, COMMISSION, WITHDRAWAL, REFUND

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "COMPLETED"; // PENDING, COMPLETED, FAILED, CANCELLED

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
