package com.myshop.service;

import com.myshop.config.MetricsConfig;
import com.myshop.constants.CacheKeys;
import com.myshop.dto.response.PagedResponse;
import com.myshop.dto.response.ProductResponse;
import com.myshop.mapper.ProductMapper;
import com.myshop.model.entity.Product;
import com.myshop.model.search.ProductSearchHit;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.search.ProductSearchRepository;
import com.myshop.service.ai.EmbeddingService;
import com.myshop.service.ai.EmbeddingUnavailableException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ProductSearchService — hybrid semantic + keyword search (Phase 8).
 *
 * READ PATH:
 * query → embed (Redis-cached 1h per normalized query) → RRF fusion of
 * tsvector rank and pgvector cosine rank in one SQL round-trip → load
 * entities by id → map to the standard ProductResponse.
 *
 * DEGRADATION: if the embedding provider is down, search silently falls
 * back to keyword-only (WARN + metric) — search never 500s because an AI
 * dependency hiccuped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final Duration QUERY_EMBEDDING_TTL = Duration.ofHours(1);
    private static final Duration SIMILAR_TTL = Duration.ofMinutes(10);

    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final EmbeddingService embeddingService;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> search(String query, int page, int size) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        int offset = page * size;

        List<ProductSearchHit> hits;
        float[] queryVector = embedQueryCached(normalized);
        String mode;
        if (queryVector != null) {
            hits = productSearchRepository.hybridSearch(normalized, queryVector, size, offset);
            mode = "hybrid";
        } else {
            hits = productSearchRepository.keywordSearch(normalized, size, offset);
            mode = "keyword";
        }
        meterRegistry.counter(MetricsConfig.SEARCHES, "mode", mode).increment();

        List<ProductResponse> content = toResponses(hits);
        // Search has no cheap exact total (RRF fuses two bounded candidate
        // lists), so totalElements is a lower bound and `last` is derived from
        // whether this page filled up — enough for infinite-scroll UIs.
        boolean last = content.size() < size;
        return PagedResponse.<ProductResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements((long) offset + content.size())
                .totalPages(last ? page + 1 : page + 2)
                .last(last)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findSimilar(UUID productId, int limit) {
        // Cheap id-list cache: vectors change rarely; 10 minutes of staleness
        // on a recommendation rail is invisible to users.
        String cacheKey = CacheKeys.format(CacheKeys.SIMILAR_PRODUCTS, productId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            List<UUID> ids = cached.isBlank() ? List.of()
                    : Arrays.stream(cached.split(",")).map(UUID::fromString).toList();
            return toResponsesOrdered(ids);
        }

        List<ProductSearchHit> hits = productSearchRepository.findSimilar(productId, limit);
        String joined = hits.stream().map(h -> h.productId().toString())
                .collect(Collectors.joining(","));
        redisTemplate.opsForValue().set(cacheKey, joined, SIMILAR_TTL);
        return toResponses(hits);
    }

    /** @return the query vector, or null when the provider is unavailable. */
    private float[] embedQueryCached(String normalizedQuery) {
        String cacheKey = CacheKeys.format(CacheKeys.SEARCH_QUERY_EMBEDDING, normalizedQuery);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            String[] parts = cached.split(",");
            float[] vector = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i]);
            }
            return vector;
        }
        try {
            float[] vector = embeddingService.embedQuery(normalizedQuery);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vector.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(vector[i]);
            }
            redisTemplate.opsForValue().set(cacheKey, sb.toString(), QUERY_EMBEDDING_TTL);
            return vector;
        } catch (EmbeddingUnavailableException e) {
            log.warn("Embedding provider unavailable — search degrading to keyword-only: {}", e.getMessage());
            return null;
        }
    }

    private List<ProductResponse> toResponses(List<ProductSearchHit> hits) {
        return toResponsesOrdered(hits.stream().map(ProductSearchHit::productId).toList());
    }

    /** Load by id and restore ranking order (findAllById gives no ordering). */
    private List<ProductResponse> toResponsesOrdered(List<UUID> orderedIds) {
        Map<UUID, Product> byId = productRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return orderedIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(productMapper::toResponse)
                .toList();
    }
}
