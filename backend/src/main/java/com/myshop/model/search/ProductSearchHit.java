package com.myshop.model.search;

import java.util.UUID;

/**
 * One hybrid-search result: the product id plus its fused ranking score
 * (Reciprocal Rank Fusion of keyword rank and vector rank). Entities are
 * loaded separately by id — search never bypasses the JPA read path.
 */
public record ProductSearchHit(UUID productId, double score) {
}
