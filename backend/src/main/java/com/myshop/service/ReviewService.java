package com.myshop.service;

import com.myshop.dto.request.ReviewRequest;
import com.myshop.dto.response.PagedResponse;
import com.myshop.dto.response.ReviewResponse;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.model.document.ActivityLogAggregationResult;
import com.myshop.model.document.ProductReview;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.OrderItemRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.repository.mongo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing product reviews entirely in MongoDB.
 * Demonstrates Polyglot Persistence:
 * - We verify the user and product exist in PostgreSQL.
 * - We verify the user actually BOUGHT the product in PostgreSQL.
 * - We save the review itself in MongoDB.
 * - Finally, we asynchronously update the aggregated stats back into
 * PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewResponse createReview(String email, UUID productId, ReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId.toString()));

        // Prevent multiple reviews from the same user
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS, "You have already reviewed this product.");
        }

        // Cross-DB logic: Did they buy this?
        // We query the PostgreSQL OrderItemRepository to verify purchase history
        // before saving the review into MongoDB.
        boolean verifiedPurchase = orderItemRepository.existsByOrderUserIdAndProductId(user.getId(), productId);

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .userId(user.getId())
                .userName(user.getFullName()) // Denormalization magic!
                .rating(request.rating())
                .title(request.title())
                .comment(request.comment())
                .verifiedPurchase(verifiedPurchase)
                .build();

        ProductReview savedReview = reviewRepository.save(review);
        log.info("Review created for product {} by user {}", productId, email);

        // ASYNC Call: Soft eventual consistency sync back to Postgres
        syncProductRatingStats(productId);

        return mapToResponse(savedReview);
    }

    public PagedResponse<ReviewResponse> getProductReviews(UUID productId, int page, int size, String sortOption) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Default
        if ("helpful".equalsIgnoreCase(sortOption)) {
            sort = Sort.by(Sort.Direction.DESC, "helpfulVotes");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductReview> reviewPage = reviewRepository.findByProductId(productId, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.of(reviewPage, content);
    }

    public ReviewResponse markReviewHelpful(String reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        return mapToResponse(reviewRepository.save(review));
    }

    public void deleteReview(String email, String reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User user = userRepository.findByEmail(email).orElseThrow();
        if (!review.getUserId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS, "You can only delete your own reviews.");
        }

        UUID productId = review.getProductId();
        reviewRepository.delete(review);
        log.info("Review {} deleted by user {}", reviewId, email);

        // Async recalculate stats
        syncProductRatingStats(productId);
    }

    public boolean canUserReviewProduct(String email, UUID productId) {
        User user = userRepository.findByEmail(email).orElseThrow();
        // Check if already reviewed
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return false;
        }
        // Check if purchased
        return orderItemRepository.existsByOrderUserIdAndProductId(user.getId(), productId);
    }

    /**
     * Executes entirely on a background thread.
     * Uses the MongoDB Aggregation Pipeline to calculate the exact average,
     * then writes it directly back to the PostgreSQL product record.
     */
    @Async("analyticsTaskExecutor")
    public CompletableFuture<Void> syncProductRatingStats(UUID productId) {
        try {
            ActivityLogAggregationResult stats = reviewRepository.getAverageRatingAndCountByProductId(productId);

            if (stats != null) {
                Product product = productRepository.findById(productId).orElseThrow();
                product.setReviewCount(stats.getReviewCount());

                // Format to 2 decimal places
                BigDecimal avg = new BigDecimal(stats.getAverageRating())
                        .setScale(2, RoundingMode.HALF_UP);
                product.setAvgRating(avg);

                productRepository.save(product);
                log.info("Successfully synced Postgres product {} rating to {} ({} reviews)",
                        productId, avg, stats.getReviewCount());
            }
        } catch (Exception e) {
            log.error("Failed to sync product ratings for {}: {}", productId, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private ReviewResponse mapToResponse(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getUserName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getHelpfulVotes(),
                review.getVerifiedPurchase(),
                review.getCreatedAt());
    }
}
