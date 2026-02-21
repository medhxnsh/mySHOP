package com.myshop.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB Document for User Activity Logs.
 * 
 * We use MongoDB because logs are high-write volume and schema-less.
 * A TTL (Time-To-Live) index is a special single-field index that MongoDB uses
 * to
 * automatically delete documents after a certain amount of time. A background
 * thread
 * in mongod reads the values in the index and removes expired documents from
 * the collection.
 * Here, documents expire after 90 days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_activity_logs")
public class UserActivityLog {

    @Id
    private String id;

    @Field("user_id")
    private UUID userId;

    private String action; // e.g. "PRODUCT_VIEWED", "CART_UPDATED"

    @Field("entity_type")
    private String entityType; // e.g. "PRODUCT", "ORDER"

    @Field("entity_id")
    private String entityId;

    // Flexible schema perfectly suited for strict JSON metadata
    private Map<String, Object> metadata;

    // TTL Index: expireAfterSeconds = 90 days (7776000 seconds)
    @Indexed(expireAfterSeconds = 7776000)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
