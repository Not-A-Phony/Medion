package com.medion.hardwarestore.controller.product;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    public record ProductDto(UUID id, String name, String description, String sku, BigDecimal price, String currency, Integer stockQuantity, UUID storeId) {}
    public record CreateProductRequest(String name, String description, String sku, BigDecimal price, String currency, Integer stockQuantity, UUID storeId) {}

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllActiveProducts().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable UUID id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(mapToDto(product));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "USD")
                .stockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0)
                .isActive(true)
                .build();
        
        Product savedProduct = productService.createProduct(product, request.storeId());
        return ResponseEntity.ok(mapToDto(savedProduct));
    }

    private ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.getStore().getId()
        );
    }
}
