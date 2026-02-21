package com.myshop.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        String paymentStatus,
        String paymentReference,
        Map<String, String> shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt) {
}
