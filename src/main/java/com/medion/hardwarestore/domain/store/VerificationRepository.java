package com.medion.hardwarestore.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, UUID> {
    Optional<Verification> findByStoreId(UUID storeId);
}
