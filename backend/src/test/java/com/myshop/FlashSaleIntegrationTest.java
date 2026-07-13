package com.myshop;

import com.myshop.constants.CacheKeys;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.kafka.event.FlashOrderEvent;
import com.myshop.model.entity.FlashSale;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.model.enums.FlashSaleStatus;
import com.myshop.repository.jpa.FlashSaleReservationRepository;
import com.myshop.repository.jpa.OrderRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.service.FlashOrderService;
import com.myshop.service.FlashPurchaseService;
import com.myshop.service.FlashSaleService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Phase 9: the no-oversell invariant, exercised against REAL Redis with the
 * REAL Lua script under genuine thread contention.
 *
 * Kafka is mocked — the hot path's only Kafka interaction is a publish, and
 * these tests assert the Redis-side accounting. The Kafka hand-off and worker
 * are covered by the worker idempotency test + the live k6 run.
 */
@SpringBootTest(properties = "myshop.outbox.relay-enabled=false")
@ActiveProfiles("test")
class FlashSaleIntegrationTest {

    @Autowired
    private FlashSaleService flashSaleService;

    @Autowired
    private FlashPurchaseService flashPurchaseService;

    @Autowired
    private FlashOrderService flashOrderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FlashSaleReservationRepository reservationRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UUID saleIdForCleanup;

    @AfterEach
    void cleanRedis() {
        if (saleIdForCleanup != null) {
            redisTemplate.delete(List.of(
                    CacheKeys.format(CacheKeys.FLASH_STOCK, saleIdForCleanup),
                    CacheKeys.format(CacheKeys.FLASH_BUYERS, saleIdForCleanup),
                    CacheKeys.format(CacheKeys.FLASH_META, saleIdForCleanup)));
        }
    }

    private void mockKafkaSuccess() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new SendResult<>(new ProducerRecord<>("t", "k", new Object()), null)));
    }

    private Product product(int stock) {
        return productRepository.save(Product.builder()
                .name("Flash Test Product " + UUID.randomUUID())
                .price(new BigDecimal("99.99"))
                .stockQuantity(stock)
                .sku("FLASH-" + UUID.randomUUID())
                .active(true)
                .build());
    }

    private FlashSale activeSale(int productStock, int saleStock) {
        Product p = product(productStock);
        FlashSale sale = flashSaleService.create(p.getId(), new BigDecimal("49.99"), saleStock,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
        sale = flashSaleService.activate(sale.getId());
        saleIdForCleanup = sale.getId();
        return sale;
    }

    @Test
    void activation_reservesProductStockAndWarmsRedis() {
        Product p = product(100);
        FlashSale sale = flashSaleService.create(p.getId(), new BigDecimal("9.99"), 30,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
        sale = flashSaleService.activate(sale.getId());
        saleIdForCleanup = sale.getId();

        assertThat(sale.getStatus()).isEqualTo(FlashSaleStatus.ACTIVE);
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStockQuantity())
                .as("sale units must move OUT of the product stock")
                .isEqualTo(70);
        assertThat(flashSaleService.remainingStock(sale.getId())).isEqualTo(30);
    }

    @Test
    void concurrentPurchases_neverOversell() throws Exception {
        mockKafkaSuccess();
        int stock = 10;
        int buyers = 50;
        FlashSale sale = activeSale(stock, stock);
        UUID saleId = sale.getId();

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(buyers);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();

        for (int i = 0; i < buyers; i++) {
            UUID userId = UUID.randomUUID(); // 50 distinct users, one attempt each
            pool.submit(() -> {
                try {
                    start.await();
                    flashPurchaseService.purchase(saleId, userId);
                    accepted.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.FLASH_SALE_SOLD_OUT) {
                        soldOut.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown(); // all 50 threads race at once
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(accepted.get()).as("exactly the stock is sold — zero oversell").isEqualTo(stock);
        assertThat(soldOut.get()).isEqualTo(buyers - stock);
        assertThat(flashSaleService.remainingStock(saleId)).isZero();
        assertThat(redisTemplate.opsForSet()
                .size(CacheKeys.format(CacheKeys.FLASH_BUYERS, saleId))).isEqualTo(stock);
    }

    @Test
    void secondPurchaseBySameUser_isRejected() {
        mockKafkaSuccess();
        FlashSale sale = activeSale(10, 5);
        UUID userId = UUID.randomUUID();

        flashPurchaseService.purchase(sale.getId(), userId);

        assertThatThrownBy(() -> flashPurchaseService.purchase(sale.getId(), userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FLASH_SALE_ALREADY_PURCHASED);

        // The rejection must NOT consume stock.
        assertThat(flashSaleService.remainingStock(sale.getId())).isEqualTo(4);
    }

    @Test
    void purchaseAgainstInactiveSale_isRejected() {
        assertThatThrownBy(() -> flashPurchaseService.purchase(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FLASH_SALE_NOT_ACTIVE);
    }

    @Test
    void worker_materializesOrderOnce_underRedelivery() {
        FlashSale sale = activeSale(10, 5);
        User user = userRepository.save(User.builder()
                .email("flash-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$x").fullName("Flash Buyer").role("USER").isActive(true)
                .build());

        FlashOrderEvent event = FlashOrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .reservationId(UUID.randomUUID())
                .saleId(sale.getId())
                .userId(user.getId())
                .productId(sale.getProduct().getId())
                .salePrice(new BigDecimal("49.99"))
                .build();

        long ordersBefore = orderRepository.count();
        flashOrderService.materializeReservation(event);
        flashOrderService.materializeReservation(event); // at-least-once redelivery

        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(reservationRepository.findById(event.getReservationId())).isPresent();
        assertThat(reservationRepository.findById(event.getReservationId()).orElseThrow()
                .getOrderId()).isNotNull();
    }
}
