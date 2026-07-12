package com.medion.hardwarestore.controller.admin;

import com.medion.hardwarestore.controller.product.ProductController.ProductDto;
import com.medion.hardwarestore.controller.store.StoreController.StoreDto;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.service.ProductService;
import com.medion.hardwarestore.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StoreService storeService;
    private final ProductService productService;

    @GetMapping("/stores")
    public ResponseEntity<List<StoreDto>> getAllStores() {
        List<StoreDto> stores = storeService.getAllStores().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @PutMapping("/stores/{id}/approve")
    public ResponseEntity<StoreDto> approveStore(@PathVariable UUID id) {
        Store store = storeService.approveStore(id);
        return ResponseEntity.ok(mapToDto(store));
    }

    @PutMapping("/stores/{id}/reject")
    public ResponseEntity<StoreDto> rejectStore(@PathVariable UUID id) {
        Store store = storeService.rejectStore(id);
        return ResponseEntity.ok(mapToDto(store));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProductsForAdmin().stream()
                .map(product -> new ProductDto(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getSku(),
                        product.getPrice(),
                        product.getCurrency(),
                        product.getStockQuantity(),
                        product.getStore().getId(),
                        product.getAverageRating(),
                        product.getReviewCount(),
                        product.getImageUrls()
                ))
                .toList();
        return ResponseEntity.ok(products);
    }

    private StoreDto mapToDto(Store store) {
        return new StoreDto(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                store.getStatus(),
                store.getAverageRating(),
                store.getReviewCount(),
                store.getIsFeatured(),
                store.getLogoUrl(),
                store.getBannerUrl(),
                store.getBio(),
                store.getAdsUrls()
        );
    }
}
