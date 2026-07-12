package com.medion.hardwarestore.domain.payment;

import com.medion.hardwarestore.domain.store.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "KES";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "tracking_id")
    private String trackingId;

    @Column(name = "merchant_reference", nullable = false, unique = true)
    private String merchantReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
