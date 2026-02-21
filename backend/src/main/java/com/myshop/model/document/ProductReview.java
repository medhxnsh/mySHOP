package com.myshop.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

/**
 * MongoDB Document for Product Reviews.
 * 
 * WHY IS userName DENORMALIZED HERE?
 * Because cross-database joins don't exist between MongoDB and PostgreSQL.
 * Denormalization is the standard polyglot persistence pattern to avoid N+1
 * cross-network fetches.
 * (This is a common MAANG interview topic!)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_reviews")
@CompoundIndexes({
        @CompoundIndex(name = "product_idx", def = "{'productId': 1}"),
        @CompoundIndex(name = "user_idx", def = "{'userId': 1}"),
        @CompoundIndex(name = "product_created_idx", def = "{'productId': 1, 'createdAt': -1}")
})
public class ProductReview {

    @Id
    private String id; // MongoDB ObjectId maps well to String

    @Field("product_id")
    private UUID productId; // References PostgreSQL product

    @Field("user_id")
    private UUID userId; // References PostgreSQL user

    @Field("user_name")
    private String userName; // Denormalized field

    private Integer rating; // 1-5

    private String title;

    private String comment;

    @Field("helpful_votes")
    @Builder.Default
    private Integer helpfulVotes = 0;

    @Field("verified_purchase")
    private Boolean verifiedPurchase;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
