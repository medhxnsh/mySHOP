package com.myshop.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable record of an accepted flash purchase, written by FlashOrderWorker.
 * The id is generated on the Redis hot path and travels through Kafka —
 * clients poll it while the worker catches up.
 */
@Entity
@Table(name = "flash_sale_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleReservation {

    @Id
    private UUID id; // assigned by the hot path, NOT generated here

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String status; // CONFIRMED | FAILED

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
