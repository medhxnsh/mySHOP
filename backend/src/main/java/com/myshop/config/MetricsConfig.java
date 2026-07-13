package com.myshop.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MetricsConfig — Phase 7 observability.
 *
 * Metric NAMES are centralized here (same rationale as KafkaTopics/CacheKeys:
 * a typo'd metric name silently creates a second, empty series — dashboards
 * query one, the code writes the other).
 *
 * Micrometer converts dots to underscores for Prometheus, so
 * "myshop.orders.placed" is queried as myshop_orders_placed_total.
 * All names are referenced by the Grafana dashboards in ops/grafana/.
 */
@Configuration
public class MetricsConfig {

    // ── Business metrics ─────────────────────────────────────────────────────
    /** Counter — orders successfully placed. */
    public static final String ORDERS_PLACED = "myshop.orders.placed";
    /** Timer — end-to-end placeOrder duration (via @Timed). */
    public static final String CHECKOUT_TIMER = "myshop.checkout";
    /** Counter — business-rule rejections, tagged {code} (ErrorCode name). */
    public static final String BUSINESS_ERRORS = "myshop.business.errors";

    // ── Outbox metrics ───────────────────────────────────────────────────────
    /** Counter — outbox events delivered to Kafka. */
    public static final String OUTBOX_PUBLISHED = "myshop.outbox.published";
    /** Counter — outbox delivery failures (attempts). */
    public static final String OUTBOX_FAILURES = "myshop.outbox.failures";
    /** Counter — rows parked after MAX_ATTEMPTS (alert-worthy). */
    public static final String OUTBOX_STUCK = "myshop.outbox.stuck";

    // ── Flash sale metrics (Phase 9) ─────────────────────────────────────────
    /** Counter — accepted flash purchases (reservation handed to Kafka). */
    public static final String FLASH_ACCEPTED = "myshop.flash.accepted";
    /** Counter — rejected flash purchases, tagged {reason}. */
    public static final String FLASH_REJECTED = "myshop.flash.rejected";
    /** Timer — hot-path latency (Lua + Kafka hand-off). */
    public static final String FLASH_PURCHASE_TIMER = "myshop.flash.purchase";
    /** Counter — reservationless drift detected by the reconciliation job. */
    public static final String FLASH_DRIFT = "myshop.flash.drift";

    // ── AI / Search metrics (Phase 8) ────────────────────────────────────────
    /** Counter — embedding provider failures (search degrades to keyword-only). */
    public static final String EMBEDDING_FAILURES = "myshop.embedding.failures";
    /** Counter — product searches, tagged {mode="hybrid"|"keyword"}. */
    public static final String SEARCHES = "myshop.search.requests";

    // ── Cache metrics ────────────────────────────────────────────────────────
    /** Counter — product cache lookups, tagged {result="hit"|"miss"}. */
    public static final String PRODUCT_CACHE = "myshop.cache.product";

    /**
     * Enables @Timed on beans (used on OrderService.placeOrder). Without this
     * aspect bean the annotation is silently ignored.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Fallback ONLY for contexts that explicitly disable tracing (the test
     * profile sets management.tracing.enabled=false, which removes the
     * auto-configured OTel Tracer): outbox components inject Tracer
     * unconditionally and get the no-op implementation.
     *
     * Deliberately NOT @ConditionalOnMissingBean — user @Configuration beans
     * register before auto-configuration, so a missing-bean condition here
     * would make the real OTel tracer back off and silently kill all tracing.
     */
    @Bean
    @ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "false")
    public Tracer noopTracer() {
        return Tracer.NOOP;
    }
}
