package com.myshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * UpdateProductRequest — partial update DTO. All fields are optional.
 *
 * WHY SEPARATE CREATE AND UPDATE DTOs?
 * - CREATE: all required fields must be present → strict validation
 * - UPDATE: only send what you want to change → PATCH semantics
 * Sharing one DTO forces all fields to be optional everywhere (bad for creates)
 * or all required everywhere (bad for updates).
 * Separate DTOs = explicit contracts for each operation.
 */
@Data
public class UpdateProductRequest {

    @Size(max = 500)
    private String name;

    private String description;

    @Positive(message = "Price must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Min(0)
    private Integer stockQuantity;

    @Size(max = 100)
    private String sku;

    @Size(max = 500)
    private String imageUrl;

    private UUID categoryId;

    private Boolean isActive;
}
