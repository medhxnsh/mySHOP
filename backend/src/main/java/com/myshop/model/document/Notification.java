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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    @Field("user_id")
    private UUID userId;

    private String type; // "ORDER_CONFIRMED", "ORDER_SHIPPED", etc.

    private String title;

    private String body;

    @Field("is_read")
    @Builder.Default
    private Boolean isRead = false;

    private Map<String, Object> metadata;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
