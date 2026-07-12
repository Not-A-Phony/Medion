package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.domain.category.Category;
import com.medion.hardwarestore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.service.ProductService;
import com.medion.hardwarestore.controller.ServiceController.ServiceDto;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllRootCategories());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Category>> getFeaturedCategories() {
        return ResponseEntity.ok(categoryService.getFeaturedCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Category>> getCategoriesByType(@PathVariable String type) {
        return ResponseEntity.ok(categoryService.getCategoriesByType(type));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category, @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(categoryService.createCategory(category, parentId));
    }

    @GetMapping("/{categoryId}/services")
    public ResponseEntity<List<ServiceDto>> getServicesByCategory(@PathVariable UUID categoryId) {
        List<ServiceDto> services = productService.getProductsByCategoryId(categoryId).stream()
                .filter(p -> p.getCategory() != null && "SERVICE".equals(p.getCategory().getType()))
                .map(this::mapToServiceDto)
                .toList();
        return ResponseEntity.ok(services);
    }

    private ServiceDto mapToServiceDto(Product product) {
        return new ServiceDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                60,
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getImageUrls(),
                product.getIsActive(),
                product.getStore() != null ? product.getStore().getName() : null,
                product.getStore() != null ? product.getStore().getId() : null,
                product.getAverageRating()
        );
    }
}
