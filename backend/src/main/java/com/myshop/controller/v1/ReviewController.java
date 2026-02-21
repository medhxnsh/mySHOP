package com.myshop.controller.v1;

import com.myshop.dto.request.ReviewRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.dto.response.ReviewResponse;
import com.myshop.service.ReviewService;
import com.myshop.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID productId,
            @Valid @RequestBody ReviewRequest request) {

        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        ReviewResponse response = reviewService.createReview(email, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/products/{productId}/reviews/eligibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> checkReviewEligibility(@PathVariable UUID productId) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        boolean canReview = reviewService.canUserReviewProduct(email, productId);
        return ResponseEntity.ok(ApiResponse.success(canReview));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recent") String sort) {

        PagedResponse<ReviewResponse> response = reviewService.getProductReviews(productId, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/reviews/{reviewId}/helpful")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> markReviewHelpful(@PathVariable String reviewId) {
        ReviewResponse response = reviewService.markReviewHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable String reviewId) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        reviewService.deleteReview(email, reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }
}
