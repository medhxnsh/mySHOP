package com.myshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CreateProductRequest — validated input for creating a new product.
 *
 * WHY @Positive for price? Price cannot be zero (free products require
 * a different business flow) and definitely not negative.
 *
 * WHY @Min(0) for stockQuantity? Zero stock is valid (out of stock).
 * Negative stock is a data integrity error.
 */
@Data
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 500, message = "Product name cannot exceed 500 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    private String sku;

    /** Optional — URL of the product image */
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    /** Optional — product will be uncategorised if null */
    private UUID categoryId;
}
