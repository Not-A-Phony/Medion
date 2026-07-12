package com.medion.hardwarestore.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreFollowerRepository extends JpaRepository<StoreFollower, UUID> {
    Optional<StoreFollower> findByStoreIdAndUserId(UUID storeId, UUID userId);
    boolean existsByStoreIdAndUserId(UUID storeId, UUID userId);
    long countByStoreId(UUID storeId);
}
