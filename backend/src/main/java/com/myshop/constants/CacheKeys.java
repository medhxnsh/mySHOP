package com.myshop.constants;

/**
 * CacheKeys — All Redis cache key patterns in one place.
 *
 * WHY NO MAGIC STRINGS?
 * If "product:{id}" appears in 5 service methods and you decide to change
 * the key format, you have to find and update all 5 places. Miss one = cache
 * bug.
 * With this constants class: change once, all references are correct.
 *
 * NAMING CONVENTION:
 * Keys use colon-separated namespacing: category:subcategory:identifier
 * Redis treats colon as a visual separator — Redis Commander/RedisInsight
 * shows keys in a tree view based on colons. Very useful for debugging.
 *
 * Added in Phase 4. Defined here in Phase 0 so all skeleton references compile.
 */
public final class CacheKeys {

    private CacheKeys() {
        throw new UnsupportedOperationException("Constants class");
    }

    // ── Product Keys ──────────────────────────────────────────────────────────

    /** Single product by ID. TTL: 10 minutes. */
    public static final String PRODUCT_BY_ID = "product:%s";

    /** Paginated product list with filter hash. TTL: 5 minutes. */
    public static final String PRODUCTS_LIST = "products:list:%s";

    // ── Category Keys ─────────────────────────────────────────────────────────

    /** Full category tree. TTL: 1 hour (rarely changes). */
    public static final String CATEGORIES_ALL = "categories:all";

    // ── Auth Keys ─────────────────────────────────────────────────────────────

    /** JWT refresh token. TTL: 7 days. */
    public static final String JWT_REFRESH = "jwt:refresh:%s";

    // ── Rate Limiting Keys ────────────────────────────────────────────────────

    /** Rate limit counter per IP per minute window. TTL: 1 minute. */
    public static final String RATE_LIMIT_API = "rate_limit:%s:%d";

    /** Login attempt counter per IP (stricter). TTL: 1 minute. */
    public static final String RATE_LIMIT_AUTH = "rate_limit:auth:%s:%d";

    // ── Distributed Lock Keys ─────────────────────────────────────────────────

    /** Lock key for stock deduction — prevents overselling. TTL: 5 seconds. */
    public static final String LOCK_STOCK = "lock:stock:%s";

    // ── Search Keys ───────────────────────────────────────────────────────────

    /** Autocomplete suggestions for a prefix. TTL: 30 minutes. */
    public static final String SEARCH_SUGGEST = "search:suggest:%s";

    // ── Utility method for formatted keys ────────────────────────────────────

    /** Format a key pattern with arguments. */
    public static String format(String pattern, Object... args) {
        return String.format(pattern, args);
    }
}
