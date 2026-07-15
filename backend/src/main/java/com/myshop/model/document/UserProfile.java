package com.myshop.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A user's personalization profile (Phase 10): the exponential moving average
 * of the embedding vectors of products they interacted with, kept normalized.
 * Built by ActivityProfileConsumer off the user.activity stream; consumed by
 * the personalized recommendation strategy (pgvector kNN with this vector).
 */
@Document(collection = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    private String userId;

    private List<Float> vector;

    private long eventCount;

    private Instant updatedAt;
}
