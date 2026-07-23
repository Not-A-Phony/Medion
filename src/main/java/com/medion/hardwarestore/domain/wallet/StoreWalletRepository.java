package com.medion.hardwarestore.domain.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreWalletRepository extends JpaRepository<StoreWallet, UUID> {
    Optional<StoreWallet> findByStoreId(UUID storeId);
}
