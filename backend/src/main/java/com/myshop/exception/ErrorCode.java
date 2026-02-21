package com.myshop.exception;

/**
 * ErrorCode — Centralized enum of all application error codes.
 *
 * WHY AN ENUM FOR ERROR CODES?
 * Using magic strings like "PRODUCT_NOT_FOUND" scattered across controllers
 * is bad because:
 * 1. No compile-time safety — typos cause runtime bugs
 * 2. No single source of truth — same error named differently in two places
 * 3. Hard to document all possible errors for API consumers
 *
 * With this enum:
 * - throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND)
 * - Compile error if code doesn't exist
 * - IDE autocomplete shows all possible errors
 * - One place to update all error messages
 *
 * Naming convention: ENTITY_REASON — e.g., PRODUCT_NOT_FOUND,
 * USER_ALREADY_EXISTS
 */
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR("An unexpected error occurred. Please try again later."),
    VALIDATION_FAILED("Request validation failed."),
    ACCESS_DENIED("You do not have permission to perform this action."),
    UNAUTHORIZED("You must be logged in to perform this action."),
    RESOURCE_NOT_FOUND("The requested resource was not found."),

    // ── User / Auth ───────────────────────────────────────────────────────────
    USER_NOT_FOUND("User not found."),
    USER_ALREADY_EXISTS("A user with this email already exists."),
    EMAIL_ALREADY_EXISTS("An account with this email address already exists."),
    INVALID_CREDENTIALS("Invalid email or password."),
    INVALID_TOKEN("Token is invalid or expired."),
    TOKEN_EXPIRED("Your session has expired. Please log in again."),
    ACCOUNT_DEACTIVATED("This account has been deactivated."),

    // ── Product ───────────────────────────────────────────────────────────────
    PRODUCT_NOT_FOUND("Product not found."),
    PRODUCT_ALREADY_EXISTS("A product with this SKU already exists."),
    SKU_ALREADY_EXISTS("A product with this SKU already exists."),
    PRODUCT_INACTIVE("This product is no longer available."),

    // ── Category ──────────────────────────────────────────────────────────────
    CATEGORY_NOT_FOUND("Category not found."),
    CATEGORY_SLUG_TAKEN("A category with this slug already exists."),
    SLUG_ALREADY_EXISTS("A category with this slug already exists."),

    // ── Cart ──────────────────────────────────────────────────────────────────
    CART_NOT_FOUND("Cart not found."),
    CART_EMPTY("Your cart is empty."),
    CART_IS_EMPTY("Your cart is empty. Cannot process checkout."),
    CART_ITEM_NOT_FOUND("Item not found in cart."),

    // ── Order ─────────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND("Order not found."),
    ORDER_CANNOT_BE_CANCELLED("This order cannot be cancelled."),
    ORDER_ALREADY_PAID("This order has already been paid."),
    INVALID_ORDER_STATE("The order is in an invalid state for this operation."),
    UNAUTHORIZED_ACCESS("You do not have permission to access this resource."),

    // ── Stock / Inventory ─────────────────────────────────────────────────────
    INSUFFICIENT_STOCK("Insufficient stock available."),
    STOCK_UPDATE_CONFLICT("Stock was updated by another request. Please try again."),

    // ── Payment ───────────────────────────────────────────────────────────────
    PAYMENT_FAILED("Payment processing failed."),
    PAYMENT_NOT_FOUND("Payment record not found."),

    // ── Review ────────────────────────────────────────────────────────────────
    REVIEW_NOT_FOUND("Review not found."),
    REVIEW_ALREADY_EXISTS("You have already reviewed this product."),
    REVIEW_NOT_VERIFIED("You can only review products you have purchased."),

    // ── Rate Limiting ─────────────────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED("Too many requests. Please wait before trying again."),

    // ── Search ────────────────────────────────────────────────────────────────
    SEARCH_FAILED("Search service is temporarily unavailable.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
