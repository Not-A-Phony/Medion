package com.medion.hardwarestore.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {
    List<Store> findByOwnerId(UUID ownerId);
}
