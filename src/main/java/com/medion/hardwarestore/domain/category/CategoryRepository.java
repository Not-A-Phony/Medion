package com.medion.hardwarestore.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNull();
    List<Category> findByType(String type);
    List<Category> findByIsFeaturedTrue();
}
