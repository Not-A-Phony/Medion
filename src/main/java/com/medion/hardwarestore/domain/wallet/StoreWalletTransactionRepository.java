package com.medion.hardwarestore.domain.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoreWalletTransactionRepository extends JpaRepository<StoreWalletTransaction, UUID> {
    Page<StoreWalletTransaction> findByStoreWalletStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);
}
