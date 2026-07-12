package com.medion.hardwarestore.controller.product;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.product.Review;
import com.medion.hardwarestore.integration.storage.FileStorageService;
import com.medion.hardwarestore.service.ProductService;
import com.medion.hardwarestore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.medion.hardwarestore.domain.user.User;
import java.time.LocalDateTime;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final FileStorageService fileStorageService;

    public record ProductDto(UUID id, String name, String description, String sku, BigDecimal price, String currency, Integer stockQuantity, UUID storeId, Double averageRating, Integer reviewCount, List<String> imageUrls) {}
    public record CreateProductRequest(String name, String description, String sku, BigDecimal price, String currency, Integer stockQuantity, UUID storeId) {}
    public record ReviewDto(UUID id, UUID userId, String firstName, String lastName, Integer rating, String comment, LocalDateTime createdAt) {}
    public record CreateReviewRequest(Integer rating, String comment) {}

    @GetMapping("/active")
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
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal User user
    ) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "USD")
                .stockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0)
                .isActive(true)
                .build();
        Product savedProduct = productService.createProduct(product, request.storeId(), user);
        return ResponseEntity.ok(mapToDto(savedProduct));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID id, 
            @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal User user
    ) {
        Product updatedDetails = Product.builder()
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "USD")
                .stockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0)
                .build();
        Product savedProduct = productService.updateProduct(id, updatedDetails, user);
        return ResponseEntity.ok(mapToDto(savedProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        productService.deleteProduct(id, user);
        return ResponseEntity.noContent().build();
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
                product.getStore().getId(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getImageUrls()
        );
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getProductReviews(@PathVariable UUID id) {
        List<ReviewDto> reviews = reviewService.getReviewsForProduct(id).stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getUser().getId(),
                        r.getUser().getFirstName(),
                        r.getUser().getLastName(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDto> addProductReview(
            @PathVariable UUID id,
            @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        Review r = reviewService.addReview(id, user, request.rating(), request.comment());
        ReviewDto dto = new ReviewDto(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getFirstName(),
                r.getUser().getLastName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ProductDto> uploadProductImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        String imageUrl = fileStorageService.uploadFile(file, "products");
        Product updatedProduct = productService.addProductImage(id, imageUrl, user);
        return ResponseEntity.ok(mapToDto(updatedProduct));
    }

    @DeleteMapping("/{id}/images")
    public ResponseEntity<ProductDto> deleteProductImage(
            @PathVariable UUID id,
            @RequestParam("imageUrl") String imageUrl,
            @AuthenticationPrincipal User user
    ) {
        Product updatedProduct = productService.removeProductImage(id, imageUrl, user);
        
        // Optionally, delete from Cloudinary as well
        // We need to extract the publicId from the URL first, which is usually the last part without extension
        try {
            String[] parts = imageUrl.split("/");
            String lastPart = parts[parts.length - 1];
            String publicId = "products/" + lastPart.substring(0, lastPart.lastIndexOf('.'));
            fileStorageService.deleteFile(publicId);
        } catch (Exception e) {
            // Log but don't fail, since we successfully removed it from the DB
        }
        
        return ResponseEntity.ok(mapToDto(updatedProduct));
    }
}
