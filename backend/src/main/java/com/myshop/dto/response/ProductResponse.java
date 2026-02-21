package com.myshop.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** ProductResponse — safe read-only view of a Product for API responses. */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String sku;
    private BigDecimal avgRating;
    private int reviewCount;
    private boolean isActive;

    /** Denormalised from the Category relationship */
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;

    private Instant createdAt;
    private Instant updatedAt;
}
