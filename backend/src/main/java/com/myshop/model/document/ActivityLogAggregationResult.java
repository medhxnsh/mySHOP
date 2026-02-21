package com.myshop.model.document;

import lombok.Data;

/**
 * Projection class used to capture the results of the
 * MongoDB aggregation pipeline in ReviewRepository.
 */
@Data
public class ActivityLogAggregationResult {
    private Double averageRating;
    private Integer reviewCount;
}
