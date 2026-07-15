package com.myshop.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * One user action on the user.activity stream (Phase 10 made this topic
 * real — it had been defined since Phase 0 but nothing published to it).
 * Consumed by ActivityProfileConsumer to build personalization profiles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityEvent {
    private String eventId;
    private UUID userId; // null for anonymous browsing
    private String action; // PRODUCT_VIEWED, CART_ADD, ORDER_PLACED, ...
    private String entityType; // PRODUCT, ORDER, ...
    private String entityId;
    private Instant occurredAt;
}
