package com.myshop.dto.response;

import java.util.List;

/**
 * Recommendations plus WHICH strategy produced them ("personalized",
 * "popular", "newest") — the frontend titles the rail accordingly and it
 * makes cold-start behavior observable in responses, not just metrics.
 */
public record RecommendationResponse(String strategy, List<ProductResponse> products) {
}
