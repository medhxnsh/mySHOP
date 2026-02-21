package com.myshop.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

/**
 * ApiResponse<T> — The standard envelope wrapping EVERY API response.
 *
 * WHY A RESPONSE ENVELOPE?
 * Without an envelope, your API returns raw data or HTTP status codes.
 * This is inconsistent and hard for frontends to process uniformly.
 * With a consistent envelope:
 * - Success: { "success": true, "data": {...}, "requestId": "..." }
 * - Error: { "success": false, "error": {...}, "requestId": "..." }
 *
 * @JsonInclude(NON_NULL) skips null fields in JSON output.
 *
 *                        WHY MANUAL GETTERS ON INNER CLASSES?
 *                        Lombok @Getter does not work reliably on static nested
 *                        classes that are
 *                        themselves inside a @Builder-annotated outer class in
 *                        Java 24.
 *                        Using explicit getters avoids the annotation
 *                        processing bug.
 *
 * @param <T> The type of the payload in the "data" field.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final ErrorDetails error;
    private final Instant timestamp;
    private final String requestId;

    /**
     * Nested class for error details.
     * Only present in error responses.
     * NOTE: Using explicit getters instead of Lombok @Getter to avoid
     * annotation processing issues with @Builder + nested static classes.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private final String code;
        private final String message;
        private final List<FieldError> details;

        @lombok.Builder
        public ErrorDetails(String code, String message, List<FieldError> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public List<FieldError> getDetails() {
            return details;
        }
    }

    /**
     * Individual field-level error (for validation failures).
     * e.g., { "field": "email", "message": "must be a valid email" }
     */
    public static class FieldError {
        private final String field;
        private final String message;

        @lombok.Builder
        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }

    // ── Factory methods (keeps controller code clean) ──────────────────────

    /**
     * Build a success response with data.
     * Controller usage: return ResponseEntity.ok(ApiResponse.success(product));
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("OK")
                .timestamp(Instant.now())
                // requestId comes from MDC — set by RequestIdFilter
                .requestId(MDC.get("requestId"))
                .build();
    }

    /** Build a success response with a custom message. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .build();
    }

    /** Build an error response. Used by GlobalExceptionHandler. */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .build();
    }

    /** Build an error response with field-level validation details. */
    public static <T> ApiResponse<T> validationError(String code, String message,
            List<FieldError> fieldErrors) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .details(fieldErrors)
                        .build())
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .build();
    }
}
