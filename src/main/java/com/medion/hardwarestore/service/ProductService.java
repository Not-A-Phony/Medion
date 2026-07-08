package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.product.ProductRepository;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.Role;
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

    public Product createProduct(Product product, UUID storeId, User user) {
        Store store = storeService.getStoreById(storeId);
        
        if (user.getRole() == Role.STORE_OWNER && !user.getId().equals(store.getOwnerId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only create products for your own store");
        }
        
        product.setStore(store);
        return productRepository.save(product);
    }

    public Product updateProduct(UUID id, Product updatedProductDetails, User user) {
        Product existingProduct = getProductById(id);
        
        if (user.getRole() == Role.STORE_OWNER && !user.getId().equals(existingProduct.getStore().getOwnerId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only update products for your own store");
        }
        
        existingProduct.setName(updatedProductDetails.getName());
        existingProduct.setDescription(updatedProductDetails.getDescription());
        existingProduct.setSku(updatedProductDetails.getSku());
        existingProduct.setPrice(updatedProductDetails.getPrice());
        existingProduct.setCurrency(updatedProductDetails.getCurrency());
        existingProduct.setStockQuantity(updatedProductDetails.getStockQuantity());
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(UUID id, User user) {
        Product product = getProductById(id);
        
        if (user.getRole() == Role.STORE_OWNER && !user.getId().equals(product.getStore().getOwnerId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only delete products for your own store");
        }
        
        product.setIsActive(false); // Soft delete
        productRepository.save(product);
    }
}
