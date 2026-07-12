package com.medion.hardwarestore.domain.store;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vendor_verifications")
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", unique = true, nullable = false)
    private Store store;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_urls", columnDefinition = "jsonb")
    private String documentUrls;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "otp_verified")
    @Builder.Default
    private Boolean otpVerified = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
