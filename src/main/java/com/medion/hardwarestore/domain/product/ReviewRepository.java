package com.medion.hardwarestore.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByProductIdOrderByCreatedAtDesc(UUID productId);
    boolean existsByProductIdAndUserId(UUID productId, UUID userId);
}
