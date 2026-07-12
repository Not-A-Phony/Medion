package com.medion.hardwarestore.domain.wallet;

import com.medion.hardwarestore.domain.user.User;
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
@Table(name = "vendor_wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", unique = true, nullable = false)
    private User vendor;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "withdrawable_balance", nullable = false)
    @Builder.Default
    private BigDecimal withdrawableBalance = BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
