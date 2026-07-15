package com.myshop.service.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.config.MetricsConfig;
import com.myshop.constants.CacheKeys;
import com.myshop.dto.response.ProductResponse;
import com.myshop.dto.response.RecommendationResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * RecommendationService — walks the strategy chain (Phase 10):
 *
 *   personalized (profile vector kNN)  →  popular (best sellers)  →  newest
 *
 * The first strategy that produces results wins; the chain order is an
 * explicit constructor decision, not annotation magic. Responses are cached
 * per user for 15 minutes — recommendations don't need to be fresher than
 * that, and it keeps the endpoint off the vector index for repeat renders.
 */
@Slf4j
@Service
public class RecommendationService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final List<RecommendationStrategy> chain;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RecommendationService(PersonalizedStrategy personalized,
            PopularityStrategy popularity,
            NewestStrategy newest,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.chain = List.of(personalized, popularity, newest);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public RecommendationResponse recommend(UUID userId, int limit) {
        String cacheKey = CacheKeys.format(CacheKeys.RECOMMENDATIONS,
                userId != null ? userId.toString() : "anon");

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<RecommendationResponse>() {
                });
            } catch (Exception e) {
                log.warn("Corrupt recommendation cache entry — recomputing: {}", e.getMessage());
            }
        }

        for (RecommendationStrategy strategy : chain) {
            List<ProductResponse> products = strategy.recommend(userId, limit);
            if (!products.isEmpty()) {
                RecommendationResponse response = new RecommendationResponse(strategy.name(), products);
                meterRegistry.counter(MetricsConfig.RECOMMENDATIONS, "strategy", strategy.name())
                        .increment();
                try {
                    redisTemplate.opsForValue().set(cacheKey,
                            objectMapper.writeValueAsString(response), CACHE_TTL);
                } catch (Exception e) {
                    log.warn("Failed to cache recommendations: {}", e.getMessage());
                }
                return response;
            }
        }
        return new RecommendationResponse("none", List.of());
    }
}
