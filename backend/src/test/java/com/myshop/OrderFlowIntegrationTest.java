package com.myshop;

import com.myshop.dto.request.OrderRequest;
import com.myshop.model.entity.Cart;
import com.myshop.model.entity.CartItem;
import com.myshop.model.entity.Order;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.CartRepository;
import com.myshop.repository.jpa.OrderRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the LIVE docker-compose Postgres (with Flyway + V2 seed data),
 * unlike the Testcontainers-based tests. Requires `docker compose up postgres
 * myshop-redis` first. Property values mirror the .env defaults; JWT_SECRET
 * and the Redis password are supplied here because this test bypasses the
 * "test" profile and .env is not visible to Maven.
 */
@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:5432/myshop",
                "spring.datasource.username=myshop_user",
                "spring.datasource.password=change_me_in_production",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "JWT_SECRET=integration-test-secret-key-32-chars-min",
                "spring.data.redis.password=${REDIS_PASSWORD:change_me_in_production}",
                "myshop.outbox.relay-enabled=false"
})
@Transactional // Rollback after each test
class OrderFlowIntegrationTest {

        @Autowired
        private OrderService orderService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private CartRepository cartRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private UserRepository userRepository;

        @Test
        void placeOrderFlow_Success() {
                // 1. Arrange data using actual seed data (from Flyway V2)
                // Find existing user from V2__seed_data.sql
                User testUser = userRepository.findByEmail("user@myshop.com") // seeded by V2__seed_data.sql
                                .orElseThrow();

                // Any active product with enough stock (the live catalog may be
                // reseeded from FakeStore, so hardcoded names are unreliable)
                Product product = productRepository.findAll().stream()
                                .filter(p -> p.isActive() && p.getStockQuantity() >= 2)
                                .findFirst()
                                .orElseThrow();

                int initialStock = product.getStockQuantity();

                // Ensure user has a cart
                Cart cart = cartRepository.findByUserId(testUser.getId())
                                .orElseGet(() -> cartRepository.save(Cart.builder().user(testUser).build()));

                // Add 2 headphones to cart
                cart.addItem(CartItem.builder()
                                .cart(cart)
                                .product(product)
                                .quantity(2)
                                .build());
                cartRepository.save(cart);

                OrderRequest req = new OrderRequest(Collections.singletonMap("city", "TestCity"), "COD");

                // 2. Act
                com.myshop.dto.response.OrderResponse placed = orderService.placeOrder(testUser.getEmail(), req);

                // 3. Assert
                // A) Verify cart is empty
                Cart verifyCart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
                assertThat(verifyCart.getItems()).isEmpty();

                // B) Verify Order was created (look up the exact order placed —
                // the live DB may hold older orders for the seeded user)
                Order verifiedOrder = orderRepository.findById(placed.id()).orElseThrow();
                assertThat(verifiedOrder.getItems()).hasSize(1);
                assertThat(verifiedOrder.getStatus()).isEqualTo(com.myshop.model.enums.OrderStatus.PENDING);
                assertThat(verifiedOrder.getTotalAmount())
                                .isEqualByComparingTo(product.getPrice().multiply(new BigDecimal(2)));

                // C) Verify Stock was decremented
                Product verifiedProduct = productRepository.findById(product.getId()).orElseThrow();
                assertThat(verifiedProduct.getStockQuantity()).isEqualTo(initialStock - 2);
        }
}
