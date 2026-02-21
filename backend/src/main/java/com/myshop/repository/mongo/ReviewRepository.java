package com.myshop.repository.mongo;

import com.myshop.model.document.ActivityLogAggregationResult;
import com.myshop.model.document.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends MongoRepository<ProductReview, String> {

    Page<ProductReview> findByProductId(UUID productId, Pageable pageable);

    /**
     * MongoDB Aggregation Pipeline Example.
     * 1. $match: Filter reviews for the given product ID.
     * 2. $group: Group all matching documents together (_id: null means group all),
     * calculate the average of 'rating' and count the documents.
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'product_id': ?0 } }",
            "{ '$group': { '_id': null, 'averageRating': { '$avg': '$rating' }, 'reviewCount': { '$sum': 1 } } }"
    })
    ActivityLogAggregationResult getAverageRatingAndCountByProductId(UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
