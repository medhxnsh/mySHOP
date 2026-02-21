package com.myshop.exception;

import lombok.Getter;

/**
 * ResourceNotFoundException — Thrown when a requested resource doesn't exist.
 *
 * WHY EXTEND RuntimeException (not checked Exception)?
 * Checked exceptions (that extend Exception) force every caller to handle them
 * with try-catch or declare them in method signatures — massive boilerplate.
 * Spring recommends RuntimeException for application exceptions because:
 * 1. Less boilerplate — don't pollute every method signature with "throws X"
 * 2. Spring's @Transactional only rolls back on RuntimeException by default
 * 3. The GlobalExceptionHandler catches them at the boundary — callers don't
 * need to
 *
 * Usage: throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, id);
 * The GlobalExceptionHandler maps this to HTTP 404 automatically.
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object identifier; // The ID or value that wasn't found

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.identifier = null;
    }

    public ResourceNotFoundException(ErrorCode errorCode, Object identifier) {
        super(String.format("%s (identifier: %s)", errorCode.getDefaultMessage(), identifier));
        this.errorCode = errorCode;
        this.identifier = identifier;
    }

    /**
     * Convenience constructor used throughout services.
     * new ResourceNotFoundException("Product", "id", "abc-123")
     * → "Product not found by id: abc-123"
     */
    public ResourceNotFoundException(String entityType, String fieldName, String value) {
        super(String.format("%s not found by %s: %s", entityType, fieldName, value));
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        this.identifier = value;
    }
}
