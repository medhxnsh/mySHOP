package com.myshop.dto.request;

import jakarta.validation.constraints.NotNull;
import com.myshop.model.enums.OrderStatus;

public record OrderStatusUpdateRequest(
                @NotNull(message = "Status is required") OrderStatus status) {
}
