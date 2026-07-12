package com.myshop;

import com.myshop.constants.KafkaTopics;
import com.myshop.dto.request.OrderRequest;
import com.myshop.dto.response.OrderResponse;
import com.myshop.exception.BusinessException;
import com.myshop.model.entity.Cart;
import com.myshop.model.entity.CartItem;
import com.myshop.model.entity.OutboxEvent;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.CartRepository;
import com.myshop.repository.jpa.OutboxEventRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 6 integration tests for the transactional outbox.
 *
 * Deliberately NOT @Transactional: placeOrder must run (and commit or roll
 * back) its own real transaction — the whole point is asserting what survives
 * an actual commit/rollback, which a test-managed transaction would mask.
 *
 * The relay is disabled so staged rows stay observable instead of racing off
 * to Kafka mid-assertion. Relay delivery is covered by KafkaIntegrationTest.
 */
@SpringBootTest(properties = "myshop.outbox.relay-enabled=false")
@ActiveProfiles("test")
class OutboxOrderFlowIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    private User user;
    private Product product;

    @BeforeEach
    void seedData() {
        user = userRepository.save(User.builder()
                .email("outbox-test-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$test.hash.not.used.by.these.tests")
                .fullName("Outbox Tester")
                .role("USER")
                .isActive(true)
                .build());

        product = productRepository.save(Product.builder()
                .name("Outbox Test Product")
                .price(new BigDecimal("25.00"))
                .stockQuantity(3)
                .sku("OUTBOX-" + UUID.randomUUID())
                .active(true)
                .build());
    }

    private void fillCart(int quantity) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
        cart.addItem(CartItem.builder().cart(cart).product(product).quantity(quantity).build());
        cartRepository.save(cart);
    }

    @Test
    void placeOrder_commitsOutboxRowsAtomicallyWithOrder() {
        fillCart(2);

        OrderResponse order = orderService.placeOrder(user.getEmail(),
                new OrderRequest(Map.of("city", "TestCity"), "COD"));

        // Both events staged, pending, and correctly addressed.
        List<OutboxEvent> orderEvents = outboxEventRepository.findAll().stream()
                .filter(e -> order.id().toString().equals(e.getAggregateId()))
                .toList();
        assertThat(orderEvents).hasSize(1);
        assertThat(orderEvents.get(0).getTopic()).isEqualTo(KafkaTopics.ORDER_PLACED);
        assertThat(orderEvents.get(0).getPartitionKey()).isEqualTo(user.getId().toString());
        assertThat(orderEvents.get(0).getPublishedAt()).isNull();

        List<OutboxEvent> inventoryEvents = outboxEventRepository.findAll().stream()
                .filter(e -> product.getId().toString().equals(e.getAggregateId()))
                .toList();
        assertThat(inventoryEvents).hasSize(1);
        assertThat(inventoryEvents.get(0).getTopic()).isEqualTo(KafkaTopics.INVENTORY_UPDATED);

        assertThat(productRepository.findById(product.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(1);
    }

    /**
     * REGRESSION TEST for the dual-write bug this phase fixes.
     *
     * Before the outbox, OrderService published the inventory event to Kafka
     * DURING the transaction — an insufficient-stock failure later in the same
     * checkout rolled back the order but the event was already on the wire:
     * consumers processed a stock change that never happened. With the outbox,
     * the rollback must erase the staged events along with everything else.
     */
    @Test
    void placeOrder_rollback_leavesNoOutboxRowsAndNoStockChange() {
        fillCart(5); // stock is only 3 — placeOrder must fail and roll back

        long outboxBefore = outboxEventRepository.count();

        assertThatThrownBy(() -> orderService.placeOrder(user.getEmail(),
                new OrderRequest(Map.of("city", "TestCity"), "COD")))
                .isInstanceOf(BusinessException.class);

        assertThat(outboxEventRepository.count())
                .as("rolled-back order must leave zero outbox rows (no ghost events)")
                .isEqualTo(outboxBefore);
        assertThat(productRepository.findById(product.getId()).orElseThrow()
                .getStockQuantity())
                .as("stock must be untouched after rollback")
                .isEqualTo(3);
    }
}
