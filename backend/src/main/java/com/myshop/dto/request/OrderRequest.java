package com.myshop.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record OrderRequest(
        @NotNull(message = "Shipping address is required") Map<String, String> shippingAddress) {
}
