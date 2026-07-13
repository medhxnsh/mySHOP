package com.myshop.dto.response;

import java.util.UUID;

/**
 * Polling view of a flash reservation: PENDING until FlashOrderWorker has
 * materialized the order, then CONFIRMED with the orderId.
 */
public record FlashReservationResponse(UUID reservationId, String status, UUID orderId) {

    public static FlashReservationResponse pending(UUID reservationId) {
        return new FlashReservationResponse(reservationId, "PENDING", null);
    }
}
