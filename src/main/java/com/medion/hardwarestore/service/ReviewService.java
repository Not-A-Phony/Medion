package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.product.Product;
import com.medion.hardwarestore.domain.product.ProductRepository;
import com.medion.hardwarestore.domain.product.Review;
import com.medion.hardwarestore.domain.product.ReviewRepository;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.exception.BusinessException;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public List<Review> getReviewsForProduct(UUID productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional
    public Review addReview(UUID productId, User user, Integer rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Rating must be between 1 and 5");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new BusinessException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(rating)
                .comment(comment)
                .build();

        Review savedReview = reviewRepository.save(review);

        // Update product rating and count
        int newReviewCount = product.getReviewCount() + 1;
        double newAverage = ((product.getAverageRating() * product.getReviewCount()) + rating) / newReviewCount;

        product.setReviewCount(newReviewCount);
        product.setAverageRating(newAverage);
        productRepository.save(product);

        return savedReview;
    }
}
