package com.myshop;

import com.myshop.dto.response.PagedResponse;
import com.myshop.dto.response.ProductResponse;
import com.myshop.model.entity.Product;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.service.ProductSearchService;
import com.myshop.service.ai.EmbeddingClient;
import com.myshop.service.ai.EmbeddingService;
import com.myshop.service.ai.FakeEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8 integration tests: hybrid search + similar products against a REAL
 * pgvector database (V1–V8 migrations applied), with deterministic fake
 * embeddings so ranking assertions are stable and no model is needed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProductSearchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pgvector = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("myshop_search_test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", pgvector::getJdbcUrl);
        registry.add("spring.datasource.username", pgvector::getUsername);
        registry.add("spring.datasource.password", pgvector::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Real schema from migrations (V8 needs the vector extension) instead of
        // the test profile's create-drop-from-entities default.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("myshop.outbox.relay-enabled", () -> "false");
    }

    @TestConfiguration
    static class FakeEmbeddings {
        @Bean
        @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return new FakeEmbeddingClient();
        }
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private ProductSearchService productSearchService;

    private UUID jacketId;
    private UUID hoodieId;
    private UUID cableId;

    @BeforeEach
    void seedProducts() {
        jacketId = createAndEmbed("Arctic Puffer Jacket ZQX",
                "Insulated winter jacket ZQX that keeps you warm in freezing cold weather");
        hoodieId = createAndEmbed("Fleece Hoodie ZQX",
                "Soft warm fleece hoodie ZQX for cold winter evenings");
        cableId = createAndEmbed("USB-C Charging Cable ZQX",
                "Braided usb cable ZQX fast charging adapter electronics");
    }

    private UUID createAndEmbed(String name, String description) {
        Product product = productRepository.save(Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal("49.99"))
                .stockQuantity(10)
                .sku("SEARCH-" + UUID.randomUUID())
                .active(true)
                .build());
        embeddingService.embedAndStore(product.getId(), name, description);
        return product.getId();
    }

    @Test
    void hybridSearch_ranksSemanticallyRelatedProductsFirst() {
        // Shares words with both apparel items ("warm", "winter", "cold") but
        // not with the cable — hybrid ranking must put apparel on top.
        PagedResponse<ProductResponse> result = productSearchService.search(
                "warm clothing for cold winter ZQX", 0, 10);

        List<UUID> ids = result.getContent().stream().map(ProductResponse::getId).toList();
        assertThat(ids).contains(jacketId, hoodieId);
        assertThat(ids.indexOf(jacketId)).isLessThan(ids.indexOf(cableId) < 0 ? Integer.MAX_VALUE : ids.indexOf(cableId));
        assertThat(ids.indexOf(hoodieId)).isLessThan(ids.indexOf(cableId) < 0 ? Integer.MAX_VALUE : ids.indexOf(cableId));
    }

    @Test
    void similarProducts_returnsNearestNeighboursExcludingSelf() {
        List<ProductResponse> similar = productSearchService.findSimilar(jacketId, 5);

        List<UUID> ids = similar.stream().map(ProductResponse::getId).toList();
        assertThat(ids).doesNotContain(jacketId);
        assertThat(ids).contains(hoodieId);
        // The hoodie (shared warm/winter vocabulary) must rank above the cable.
        assertThat(ids.indexOf(hoodieId))
                .isLessThan(ids.indexOf(cableId) < 0 ? Integer.MAX_VALUE : ids.indexOf(cableId));
    }

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Test
    void search_degradesToKeywordOnly_whenEmbeddingProviderIsDown() {
        // The query vector must not be served from Redis, or the degradation
        // path is silently skipped (cache survives across test runs).
        String query = "fleece hoodie ZQX";
        redisTemplate.delete(com.myshop.constants.CacheKeys.format(
                com.myshop.constants.CacheKeys.SEARCH_QUERY_EMBEDDING, query.toLowerCase()));

        double keywordSearchesBefore = meterRegistry
                .counter(com.myshop.config.MetricsConfig.SEARCHES, "mode", "keyword").count();

        FakeEmbeddingClient client = (FakeEmbeddingClient) embeddingClient;
        client.setUnavailable(true);
        try {
            PagedResponse<ProductResponse> result = productSearchService.search(query, 0, 10);

            // No 500, keyword half still finds the product, and the service
            // actually took the degraded path (not a cached hybrid).
            assertThat(result.getContent().stream().map(ProductResponse::getId))
                    .contains(hoodieId);
            assertThat(meterRegistry.counter(com.myshop.config.MetricsConfig.SEARCHES,
                    "mode", "keyword").count())
                    .isEqualTo(keywordSearchesBefore + 1);
        } finally {
            client.setUnavailable(false);
        }
    }
}
