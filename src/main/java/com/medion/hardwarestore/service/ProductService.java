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
        return productRepository.findRandomActiveProducts();
    }

    public List<Product> getProductsByStoreId(UUID storeId) {
        return productRepository.findByStoreId(storeId);
    }

    public List<Product> getProductsByCategoryId(UUID categoryId) {
        // We will fetch all active products and filter in memory for simplicity, 
        // or we could add a method to productRepository. 
        // Since this is a simple backend, filtering in memory is fine for now, 
        // or we can use productRepository.findByCategoryId(categoryId) if it exists.
        return productRepository.findAll().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                .toList();
    }

    public List<Product> getAllProductsForAdmin() {
        return productRepository.findAll();
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    public Product createProduct(Product product, UUID storeId, User user) {
        Store store = storeService.getStoreById(storeId);
        
        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(store.getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only create products for your own store");
        }
        
        product.setStore(store);
        return productRepository.save(product);
    }

    public Product updateProduct(UUID id, Product updatedProductDetails, User user) {
        Product existingProduct = getProductById(id);
        
        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(existingProduct.getStore().getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only update products for your own store");
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
        
        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(product.getStore().getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only delete products for your own store");
        }
        
        product.setIsActive(false); // Soft delete
        productRepository.save(product);
    }

    public Product addProductImage(UUID productId, String imageUrl, User user) {
        Product product = getProductById(productId);

        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(product.getStore().getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only add images for your own store's products");
        }

        product.getImageUrls().add(imageUrl);
        return productRepository.save(product);
    }

    public Product removeProductImage(UUID productId, String imageUrl, User user) {
        Product product = getProductById(productId);

        if (user.getRole() == Role.STORE_VENDOR && !user.getId().equals(product.getStore().getOwnerId())) {
            throw new com.medion.hardwarestore.exception.BusinessException("You can only remove images for your own store's products");
        }

        product.getImageUrls().remove(imageUrl);
        return productRepository.save(product);
    }
}
