package com.myshop.service;

import com.myshop.dto.request.ReviewRequest;
import com.myshop.dto.response.ReviewResponse;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.model.document.ActivityLogAggregationResult;
import com.myshop.model.document.ProductReview;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.OrderItemRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.repository.mongo.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

        @Mock
        private ReviewRepository reviewRepository;

        @Mock
        private ProductRepository productRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private OrderItemRepository orderItemRepository;

        @InjectMocks
        private ReviewService reviewService;

        @Test
        void createReview_Success() {
                // Arrange
                UUID productId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                String email = "test@example.com";

                Product product = Product.builder().id(productId).build();
                User user = User.builder().id(userId).email(email).fullName("Test User").build();
                ReviewRequest request = new ReviewRequest(5, "Title", "Comment");

                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
                when(orderItemRepository.existsByOrderUserIdAndProductId(userId, productId)).thenReturn(true);

                ProductReview savedReview = ProductReview.builder()
                                .id("mongo-123")
                                .productId(productId)
                                .userId(userId)
                                .rating(5)
                                .title("Title")
                                .comment("Comment")
                                .verifiedPurchase(true)
                                .userName("Test User")
                                .build();

                when(reviewRepository.save(any(ProductReview.class))).thenReturn(savedReview);

                ActivityLogAggregationResult aggResult = new ActivityLogAggregationResult();
                aggResult.setReviewCount(1);
                aggResult.setAverageRating(5.0);
                when(reviewRepository.getAverageRatingAndCountByProductId(productId)).thenReturn(aggResult);

                // Act
                ReviewResponse response = reviewService.createReview(email, productId, request);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo("mongo-123");
                assertThat(response.rating()).isEqualTo(5);
                assertThat(response.verifiedPurchase()).isTrue();

                verify(reviewRepository).save(any(ProductReview.class));
                verify(productRepository).save(any(Product.class)); // Verifies async sync was called
        }

        @Test
        void createReview_ProductNotFound() {
                UUID productId = UUID.randomUUID();
                String email = "test@example.com";
                User user = User.builder().id(UUID.randomUUID()).email(email).build();

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(productRepository.findById(productId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> reviewService.createReview("test@example.com", productId,
                                new ReviewRequest(5, "", "")))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void createReview_DuplicateReview() {
                UUID productId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                String email = "test@example.com";
                User user = User.builder().id(userId).email(email).build();
                Product product = Product.builder().id(productId).build();

                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

                assertThatThrownBy(() -> reviewService.createReview(email, productId,
                                new ReviewRequest(5, "", "")))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("You have already reviewed this product");
        }
}
