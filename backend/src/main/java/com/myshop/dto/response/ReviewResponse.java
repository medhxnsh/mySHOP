package com.myshop.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        String id,
        UUID productId,
        UUID userId,
        String userName,
        Integer rating,
        String title,
        String comment,
        Integer helpfulVotes,
        Boolean verifiedPurchase,
        Instant createdAt) {
}
