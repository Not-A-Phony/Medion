package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.service.ServiceEntity;
import com.medion.hardwarestore.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/products")
    public ResponseEntity<Page<Product>> searchProducts(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(searchService.searchProducts(keyword, pageable));
    }

    @GetMapping("/services")
    public ResponseEntity<Page<ServiceEntity>> searchServices(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(searchService.searchServices(keyword, pageable));
    }

    // Nearby search is simplified in this version, could be extended using latitude and longitude
}
