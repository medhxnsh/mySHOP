package com.myshop.exception;

import lombok.Getter;

/**
 * InsufficientStockException — Thrown during order placement when requested
 * quantity exceeds available stock.
 *
 * This is a separate exception class (not just BusinessException) so that:
 * 1. Callers can catch SPECIFICALLY this exception type (e.g., to show
 * "out of stock" UI vs a generic business error)
 * 2. The GlobalExceptionHandler can return a more specific error structure
 * including which product ran out and how much stock is available
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.INSUFFICIENT_STOCK;
    private final String productId;
    private final String productName;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productId, String productName,
            int requested, int available) {
        super(String.format(
                "Insufficient stock for product '%s': requested %d, available %d",
                productName, requested, available));
        this.productId = productId;
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }
}
