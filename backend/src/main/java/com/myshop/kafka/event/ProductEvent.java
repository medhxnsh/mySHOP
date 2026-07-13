package com.myshop.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Published (via the outbox) whenever a product's content changes.
 * Carries the text needed for embedding so EmbeddingConsumer doesn't have
 * to read the product row back (the event is self-contained).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventId;
    private UUID productId;
    private String name;
    private String description;
    private boolean active;
}
