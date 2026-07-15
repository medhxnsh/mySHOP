package com.myshop.service.recommendation;

import com.myshop.dto.response.ProductResponse;

import java.util.List;
import java.util.UUID;

/**
 * One way of producing recommendations (Phase 10).
 *
 * Strategies are chained by RecommendationService: each returns an empty list
 * when it can't serve this user (no profile yet, no sales history, ...) and
 * the chain falls through. Open/Closed: a future collaborative-filtering
 * strategy is a new class in the chain — zero changes to the service.
 */
public interface RecommendationStrategy {

    /** Tag reported in the API response and metrics. */
    String name();

    /** @param userId null for anonymous visitors */
    List<ProductResponse> recommend(UUID userId, int limit);
}
