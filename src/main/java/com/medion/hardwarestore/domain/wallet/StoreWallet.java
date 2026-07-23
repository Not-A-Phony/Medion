package com.medion.hardwarestore.domain.wallet;

import com.medion.hardwarestore.domain.store.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Store-keyed wallet for subscription/marketplace earnings.
 * Separate from the vendor-keyed {@link Wallet} (vendor_wallets table).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_wallets")
public class StoreWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", unique = true, nullable = false)
    private Store store;

    @Column(name = "available_balance", nullable = false)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", nullable = false)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "total_earnings", nullable = false)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "total_commissions_paid", nullable = false)
    @Builder.Default
    private BigDecimal totalCommissionsPaid = BigDecimal.ZERO;

    @Column(name = "last_withdrawal_date")
    private LocalDateTime lastWithdrawalDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
