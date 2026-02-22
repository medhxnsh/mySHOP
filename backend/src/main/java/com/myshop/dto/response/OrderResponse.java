package com.myshop.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.myshop.model.enums.OrderStatus;
import com.myshop.model.enums.PaymentStatus;

public record OrderResponse(
                UUID id,
                UUID userId,
                OrderStatus status,
                BigDecimal totalAmount,
                PaymentStatus paymentStatus,
                String paymentReference,
                Map<String, String> shippingAddress,
                List<OrderItemResponse> items,
                Instant createdAt) {
}
