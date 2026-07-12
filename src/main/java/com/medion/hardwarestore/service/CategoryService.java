package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.category.Category;
import com.medion.hardwarestore.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getCategoriesByType(String type) {
        return categoryRepository.findByType(type.toUpperCase());
    }

    public List<Category> getFeaturedCategories() {
        return categoryRepository.findByIsFeaturedTrue();
    }

    public Category createCategory(Category category, UUID parentId) {
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            category.setParent(parent);
        }
        
        // Generate slug if empty
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            category.setSlug(generateSlug(category.getName()));
        }
        
        return categoryRepository.save(category);
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
