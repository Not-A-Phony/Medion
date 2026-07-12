package com.medion.hardwarestore.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.store.status = 'APPROVED' ORDER BY RANDOM()")
    List<Product> findRandomActiveProducts();

    List<Product> findByStoreId(UUID storeId);
}
