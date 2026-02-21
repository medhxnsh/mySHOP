package com.myshop;

import com.myshop.dto.request.ReviewRequest;
import com.myshop.dto.response.ReviewResponse;
import com.myshop.model.entity.Category;
import com.myshop.model.entity.Order;
import com.myshop.model.entity.OrderItem;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.CategoryRepository;
import com.myshop.repository.jpa.OrderItemRepository;
import com.myshop.repository.jpa.OrderRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.repository.mongo.ReviewRepository;
import com.myshop.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfSystemProperty(named = "integration.tests", matches = "true")
class ReviewServiceIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setup() {
        reviewRepository.deleteAll(); // Clean Mongo collection before each test

        testUser = userRepository.save(User.builder()
                .email("reviewer@example.com")
                .passwordHash("hash")
                .fullName("Test Reviewer")
                .build());

        Category cat = categoryRepository.save(Category.builder()
                .name("Test Category")
                .slug("test-cat-" + UUID.randomUUID())
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Amazing Laptop")
                .price(new BigDecimal("999.99"))
                .sku("LAPTOP-" + UUID.randomUUID())
                .stockQuantity(10)
                .category(cat)
                .build());

        // Create an order so the user is marked as a "verified buyer"
        Order order = orderRepository.save(Order.builder()
                .user(testUser)
                .totalAmount(new BigDecimal("999.99"))
                .shippingAddress(java.util.Map.of("city", "Test City"))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .product(testProduct)
                .quantity(1)
                .unitPrice(new BigDecimal("999.99"))
                .subtotal(new BigDecimal("999.99"))
                .build());
    }

    @Test
    void createReview_Success() {
        // Arrange
        ReviewRequest request = new ReviewRequest(5, "Great product!", "I really love this laptop.");

        // Act
        ReviewResponse response = reviewService.createReview(testUser.getEmail(), testProduct.getId(), request);

        // Assert Mongo Document
        assertThat(response.id()).isNotNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.verifiedPurchase()).isTrue(); // Verified correctly!
        assertThat(response.userName()).isEqualTo("Test Reviewer");

        // Let background async thread run (simplistic wait for integration test without
        // advanced latching)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert Postgres Product Entity (Soft Sync working)
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getReviewCount()).isEqualTo(1);
        assertThat(updatedProduct.getAvgRating()).isEqualByComparingTo(new BigDecimal("5.00"));
    }
}
