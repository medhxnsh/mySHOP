package com.myshop.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import com.myshop.model.enums.OrderStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventId;
    private UUID orderId;
    private UUID userId;
    private String email;
    private BigDecimal totalAmount;
    private OrderStatus status;
}
