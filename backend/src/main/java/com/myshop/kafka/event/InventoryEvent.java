package com.myshop.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private String eventId;
    private UUID productId;
    private String name;
    private int oldQuantity;
    private int newQuantity;
    private String reason; // e.g. "ORDER_PLACED", "ORDER_CANCELLED"
}
