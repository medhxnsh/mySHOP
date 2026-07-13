package com.myshop.model.enums;

/** Flash-sale lifecycle: DRAFT → ACTIVE (stock reserved, Redis warmed) → ENDED. */
public enum FlashSaleStatus {
    DRAFT,
    ACTIVE,
    ENDED
}
