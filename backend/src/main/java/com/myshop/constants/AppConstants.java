package com.myshop.constants;

/**
 * AppConstants — General application-wide constant values.
 */
public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    /**
     * String form required by @RequestParam(defaultValue=...) which only accepts
     * String literals
     */
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE_STR = "20";
    public static final String DEFAULT_SORT_FIELD = "createdAt";

    // JWT header constants
    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_BEARER_PREFIX = "Bearer ";

    // API versioning prefix
    public static final String API_V1 = "/api/v1";

    // User roles
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
}
