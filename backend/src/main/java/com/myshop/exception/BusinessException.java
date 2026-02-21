package com.myshop.exception;

import lombok.Getter;

/**
 * BusinessException — Thrown when a business rule is violated.
 *
 * Examples:
 * - User tries to cancel an already-shipped order
 * - User tries to place an order with an empty cart
 * - User tries to pay for an already-paid order
 *
 * Difference from ResourceNotFoundException:
 * - ResourceNotFoundException: the resource simply doesn't exist (HTTP 404)
 * - BusinessException: the resource exists but the operation is invalid (HTTP
 * 422)
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
