package com.medion.hardwarestore.domain.store;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String city;

    private String country;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_hours", columnDefinition = "jsonb")
    private String businessHours;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ads_urls", columnDefinition = "jsonb")
    private List<String> adsUrls;

    @Column(name = "verification_status")
    @Builder.Default
    private String verificationStatus = "PENDING";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StoreStatus status = StoreStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type")
    @Builder.Default
    private SubscriptionType subscriptionType = SubscriptionType.COMMISSION;

    @Column(name = "commission_rate")
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("5.0"); // Default 5%

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
