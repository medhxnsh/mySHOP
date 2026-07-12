package com.myshop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * IdempotencyException — thrown by IdempotencyAspect.
 *
 * Not a BusinessException because idempotency failures need their own HTTP
 * statuses: a missing key is a malformed request (400), and a key that is
 * currently being processed by another in-flight request is a conflict (409)
 * — neither is the 422 that BusinessException is mapped to.
 */
@Getter
public class IdempotencyException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public IdempotencyException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static IdempotencyException missingKey() {
        return new IdempotencyException(
                ErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                HttpStatus.BAD_REQUEST,
                ErrorCode.IDEMPOTENCY_KEY_REQUIRED.getDefaultMessage());
    }

    public static IdempotencyException invalidKey(String reason) {
        return new IdempotencyException(
                ErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                HttpStatus.BAD_REQUEST,
                "Invalid Idempotency-Key header: " + reason);
    }

    public static IdempotencyException conflict() {
        return new IdempotencyException(
                ErrorCode.IDEMPOTENCY_CONFLICT,
                HttpStatus.CONFLICT,
                ErrorCode.IDEMPOTENCY_CONFLICT.getDefaultMessage());
    }
}
