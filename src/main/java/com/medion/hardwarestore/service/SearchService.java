package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.product.ProductRepository;
import com.medion.hardwarestore.domain.service.ServiceEntity;
import com.medion.hardwarestore.domain.service.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;

    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
            );
        };
        return productRepository.findAll(spec, pageable);
    }

    public Page<ServiceEntity> searchServices(String keyword, Pageable pageable) {
        Specification<ServiceEntity> spec = (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            return cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
            );
        };
        return serviceRepository.findAll(spec, pageable);
    }
}
