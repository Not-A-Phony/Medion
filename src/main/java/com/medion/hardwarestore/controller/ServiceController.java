package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ProductService productService;

    public record ServiceDto(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            Integer durationMinutes,
            UUID categoryId,
            List<String> imageUrls,
            Boolean active,
            String storeName,
            UUID storeId,
            Double rating
    ) {}

    @GetMapping("/active")
    public ResponseEntity<List<ServiceDto>> getActiveServices() {
        // Find all active products where category type is SERVICE. 
        // For now, we return all products for simplicity or ideally filter by category.
        List<ServiceDto> services = productService.getAllActiveProducts().stream()
                .filter(p -> p.getCategory() != null && "SERVICE".equals(p.getCategory().getType()))
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable UUID id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(mapToDto(product));
    }

    private ServiceDto mapToDto(Product product) {
        return new ServiceDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                60, // Default duration
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getImageUrls(),
                product.getIsActive(),
                product.getStore() != null ? product.getStore().getName() : null,
                product.getStore() != null ? product.getStore().getId() : null,
                product.getAverageRating()
        );
    }
}
