package com.myshop.exception;

import com.myshop.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — Catches all exceptions thrown from
 * controllers/services
 * and converts them into standardized ApiResponse error envelopes.
 *
 * HOW DOES THIS WORK?
 * 
 * @RestControllerAdvice is @ControllerAdvice + @ResponseBody.
 *                       - @ControllerAdvice: Spring registers this class as a
 *                       global handler that
 *                       intercepts exceptions AFTER they bubble up through the
 *                       filter chain
 *                       and controller layer.
 *                       - When a controller method throws an exception, Spring
 *                       looks for an
 * @ExceptionHandler method here that handles that exception type.
 *
 *                   WHY NOT TRY-CATCH IN EVERY CONTROLLER?
 *                   That's the old way. Imagine: 20 controllers × 5 methods
 *                   each × 3 exception
 *                   types = 300 try-catch blocks. A change in error format
 *                   requires 300 edits.
 *                   GlobalExceptionHandler: change once, all 300 cases are
 *                   fixed.
 *
 *                   EXCEPTION HIERARCHY (most specific first → least specific
 *                   last):
 *                   Spring calls the MOST SPECIFIC matching handler. Order:
 *                   1. InsufficientStockException → 422 (most specific, checked
 *                   first)
 *                   2. ResourceNotFoundException → 404
 *                   3. BusinessException → 422
 *                   4. MethodArgumentNotValidException → 400 (Spring's own
 *                   validation exception)
 *                   5. AccessDeniedException → 403 (Spring Security)
 *                   6. Exception → 500 (catch-all, last resort)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 Not Found — Resource doesn't exist.
     * Triggered by: throw new
     * ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, id)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {} (identifier: {})",
                ex.getErrorCode(), ex.getIdentifier());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ex.getErrorCode().name(),
                        ex.getMessage()));
    }

    /**
     * 422 Unprocessable Entity — Business rule violated.
     * Triggered by: throw new
     * BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED)
     *
     * WHY 422 not 400?
     * 400 Bad Request = syntactically malformed request (missing fields, wrong
     * type).
     * 422 Unprocessable Entity = request is syntactically valid but semantically
     * wrong.
     * The cart is empty isn't a badly formatted request — it's a business rule
     * violation.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getErrorCode().name(),
                        ex.getMessage()));
    }

    /**
     * 422 Unprocessable Entity — Insufficient stock during order placement.
     * Returns detailed info: which product, how much was requested, how much
     * available.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: product={}, requested={}, available={}",
                ex.getProductId(), ex.getRequested(), ex.getAvailable());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getErrorCode().name(),
                        ex.getMessage()));
    }

    /**
     * 400 Bad Request — @Valid annotation on @RequestBody failed.
     *
     * HOW BEAN VALIDATION WORKS:
     * 1. Controller method has @Valid @RequestBody SomeRequest req
     * 2. Spring calls Hibernate Validator on the request object
     * 3. If any @NotNull, @Size, @Email etc. constraint fails, Spring throws
     * MethodArgumentNotValidException before the controller method even runs
     * 4. We catch it here and return all field errors in a structured list
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ApiResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed: {} field errors", fieldErrors.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(
                        ErrorCode.VALIDATION_FAILED.name(),
                        "Request validation failed. Check the 'details' field for specifics.",
                        fieldErrors));
    }

    /**
     * 403 Forbidden — User is authenticated but lacks permission.
     * Triggered by: @PreAuthorize("hasRole('ADMIN')") when user is not an admin.
     *
     * WHY HANDLE AccessDeniedException SEPARATELY?
     * Spring Security throws AccessDeniedException, which if uncaught would
     * return a plain 403 HTML error page — not our JSON envelope format.
     * We catch it here to return proper JSON.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        ErrorCode.ACCESS_DENIED.name(),
                        ErrorCode.ACCESS_DENIED.getDefaultMessage()));
    }

    /**
     * 500 Internal Server Error — Catch-all for unexpected exceptions.
     *
     * WHY LOG AT ERROR LEVEL?
     * This is an unhandled exception — something unexpected broke.
     * log.error() should alert on-call engineers. All other handlers
     * use log.warn() because they're expected/known scenarios.
     *
     * WHY HIDE THE CAUSE IN THE RESPONSE?
     * Stack traces contain internal details (file names, class structure)
     * that could help an attacker. Always return a generic message.
     * The REAL cause is in the server logs with the requestId correlation.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()));
    }
}
