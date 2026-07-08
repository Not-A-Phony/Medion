package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.product.ProductRepository;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreService storeService;

    @Cacheable(value = "products")
    public List<Product> getAllActiveProducts() {
        return productRepository.findAll().stream()
                .filter(Product::getIsActive)
                .toList();
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    public Product createProduct(Product product, UUID storeId) {
        Store store = storeService.getStoreById(storeId);
        product.setStore(store);
        return productRepository.save(product);
    }

    public Product updateProduct(UUID id, Product updatedProductDetails) {
        Product existingProduct = getProductById(id);
        existingProduct.setName(updatedProductDetails.getName());
        existingProduct.setDescription(updatedProductDetails.getDescription());
        existingProduct.setSku(updatedProductDetails.getSku());
        existingProduct.setPrice(updatedProductDetails.getPrice());
        existingProduct.setCurrency(updatedProductDetails.getCurrency());
        existingProduct.setStockQuantity(updatedProductDetails.getStockQuantity());
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.setIsActive(false); // Soft delete
        productRepository.save(product);
    }
}
