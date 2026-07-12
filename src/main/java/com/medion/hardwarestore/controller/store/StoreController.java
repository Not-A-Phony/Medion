package com.medion.hardwarestore.controller.store;

import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreFollower;
import com.medion.hardwarestore.domain.store.StoreFollowerRepository;
import com.medion.hardwarestore.domain.store.StoreStatus;
import com.medion.hardwarestore.service.StoreService;
import com.medion.hardwarestore.integration.payment.MpesaGatewayService;
import com.medion.hardwarestore.service.ProductService;
import com.medion.hardwarestore.controller.product.ProductController.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.medion.hardwarestore.domain.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ProductService productService;
    private final MpesaGatewayService mpesaService;
    private final com.medion.hardwarestore.domain.payment.StorePaymentRepository storePaymentRepository;
    private final StoreFollowerRepository storeFollowerRepository;
    private final JdbcTemplate jdbcTemplate;

    public record StoreDto(
            UUID id, String name, String address, Double latitude, Double longitude, 
            StoreStatus status, Double averageRating, Integer reviewCount, Boolean isFeatured,
            String logoUrl, String bannerUrl, String bio, List<String> adsUrls) {}
    
    public record CreateStoreResponse(StoreDto store, String mpesaStatus) {}
    
    public record CreateStoreRequest(
            @NotBlank(message = "Store name is required") String name, 
            @NotBlank(message = "Store address is required") String address, 
            @NotNull(message = "Latitude is required") Double latitude, 
            @NotNull(message = "Longitude is required") Double longitude, 
            com.medion.hardwarestore.domain.store.SubscriptionType subscriptionType) {}

    public record StoreAnalyticsResponse(
            long totalFollowers,
            long totalProducts,
            long totalOrders,
            BigDecimal totalSales
    ) {}

    @GetMapping
    public ResponseEntity<List<StoreDto>> getAllStores() {
        List<StoreDto> stores = storeService.getAllActiveStores().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> getStoreById(@PathVariable UUID id) {
        Store store = storeService.getStoreById(id);
        return ResponseEntity.ok(mapToDto(store));
    }

    @PostMapping
    public ResponseEntity<CreateStoreResponse> createStore(
            @Valid @RequestBody CreateStoreRequest request,
            @AuthenticationPrincipal User user
    ) {
        com.medion.hardwarestore.domain.store.SubscriptionType subType = request.subscriptionType() != null ? request.subscriptionType() : com.medion.hardwarestore.domain.store.SubscriptionType.COMMISSION;
        
        Store store = Store.builder()
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .subscriptionType(subType)
                .isActive(true)
                .status(subType == com.medion.hardwarestore.domain.store.SubscriptionType.COMMISSION ? StoreStatus.PENDING : StoreStatus.PENDING_PAYMENT)
                .build();
        
        Store savedStore = storeService.createStore(store, user.getId());
        
        String mpesaStatus = null;
        if (subType != com.medion.hardwarestore.domain.store.SubscriptionType.COMMISSION) {
            com.medion.hardwarestore.domain.payment.StorePayment payment = com.medion.hardwarestore.domain.payment.StorePayment.builder()
                    .store(savedStore)
                    .amount(java.math.BigDecimal.valueOf(1000))
                    .currency("KES")
                    .status(com.medion.hardwarestore.domain.payment.PaymentStatus.PENDING)
                    .merchantReference(UUID.randomUUID().toString())
                    .build();
            
            payment = storePaymentRepository.save(payment);
            String checkoutRequestId = mpesaService.initiateStkPush(user.getPhoneNumber(), payment.getAmount().longValue(), payment.getMerchantReference());
            
            if (checkoutRequestId != null) {
                payment.setTrackingId(checkoutRequestId);
                storePaymentRepository.save(payment);
            }
            
            mpesaStatus = (checkoutRequestId != null) ? "STK_PUSH_SENT" : "STK_PUSH_FAILED";
        }
        
        return ResponseEntity.ok(new CreateStoreResponse(mapToDto(savedStore), mpesaStatus));
    }
    
    @GetMapping("/my-stores")
    public ResponseEntity<List<StoreDto>> getMyStores(@AuthenticationPrincipal User user) {
        List<StoreDto> stores = storeService.getStoresByOwnerId(user.getId()).stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/my-store")
    public ResponseEntity<StoreDto> getMyStore(@AuthenticationPrincipal User user) {
        List<Store> stores = storeService.getStoresByOwnerId(user.getId());
        if (stores.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDto(stores.get(0)));
    }

    @GetMapping("/{storeId}/products")
    public ResponseEntity<List<ProductDto>> getStoreProducts(@PathVariable UUID storeId) {
        List<ProductDto> products = productService.getProductsByStoreId(storeId).stream()
                .map(p -> new ProductDto(
                        p.getId(), p.getName(), p.getDescription(), p.getSku(),
                        p.getPrice(), p.getCurrency(), p.getStockQuantity(),
                        p.getStore().getId(), p.getAverageRating(), p.getReviewCount(), p.getImageUrls()
                ))
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/nearest")
    public ResponseEntity<StoreDto> getNearestStore(
            @RequestParam double lat,
            @RequestParam double lon) {
        Store nearest = storeService.findNearestStore(lat, lon);
        return ResponseEntity.ok(mapToDto(nearest));
    }

    @PostMapping("/{storeId}/follow")
    public ResponseEntity<?> followStore(@PathVariable UUID storeId, @AuthenticationPrincipal User user) {
        if (!storeFollowerRepository.existsByStoreIdAndUserId(storeId, user.getId())) {
            Store store = storeService.getStoreById(storeId);
            StoreFollower follower = StoreFollower.builder().store(store).user(user).build();
            storeFollowerRepository.save(follower);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{storeId}/unfollow")
    public ResponseEntity<?> unfollowStore(@PathVariable UUID storeId, @AuthenticationPrincipal User user) {
        Optional<StoreFollower> follower = storeFollowerRepository.findByStoreIdAndUserId(storeId, user.getId());
        follower.ifPresent(storeFollowerRepository::delete);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{storeId}/analytics")
    public ResponseEntity<StoreAnalyticsResponse> getStoreAnalytics(@PathVariable UUID storeId) {
        long followers = storeFollowerRepository.countByStoreId(storeId);
        
        Long products = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE store_id = ?", Long.class, storeId);
        Long orders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_items WHERE store_id = ?", Long.class, storeId);
        BigDecimal sales = jdbcTemplate.queryForObject("SELECT SUM(price * quantity) FROM order_items WHERE store_id = ?", BigDecimal.class, storeId);
        
        return ResponseEntity.ok(new StoreAnalyticsResponse(
                followers,
                products != null ? products : 0,
                orders != null ? orders : 0,
                sales != null ? sales : BigDecimal.ZERO
        ));
    }

    private StoreDto mapToDto(Store store) {
        return new StoreDto(
                store.getId(), store.getName(), store.getAddress(),
                store.getLatitude(), store.getLongitude(), store.getStatus(),
                store.getAverageRating(), store.getReviewCount(), store.getIsFeatured(),
                store.getLogoUrl(), store.getBannerUrl(), store.getBio(), store.getAdsUrls()
        );
    }
}
