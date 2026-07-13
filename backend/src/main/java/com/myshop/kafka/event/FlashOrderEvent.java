package com.myshop.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A flash-sale reservation accepted on the Redis hot path. Self-contained:
 * the worker builds the order from this without re-reading the sale row.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashOrderEvent {
    private String eventId;
    private UUID reservationId;
    private UUID saleId;
    private UUID userId;
    private UUID productId;
    private BigDecimal salePrice;
}
