package com.myshop.constants;

/**
 * KafkaTopics — All Kafka topic name constants in one place.
 *
 * WHY NOT HARD-CODE TOPIC NAMES?
 * Topic names appear in producers, consumers, and configuration.
 * A typo like "order.place" vs "order.placed" creates a silent bug:
 * producer sends to one topic, consumer listens to another — messages lost
 * forever.
 * Constants catch this at compile time.
 *
 * NAMING CONVENTION:
 * Resource.action (dot-separated, lowercase)
 * Dead Letter Topics = original topic + ".DLT" (Spring Kafka convention)
 *
 * Added in Phase 5. Defined here in Phase 0 so all skeleton references compile.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new UnsupportedOperationException("Constants class");
    }

    // ── Core Topics ───────────────────────────────────────────────────────────

    /** Published when a user places an order. Key = userId (ensures ordering). */
    public static final String ORDER_PLACED = "order.placed";

    /**
     * Published when order status changes (PENDING → SHIPPED etc.). Key = orderId.
     */
    public static final String ORDER_STATUS_UPDATED = "order.status.updated";

    /** Published when product stock quantity changes. Key = productId. */
    public static final String INVENTORY_UPDATED = "inventory.updated";

    /** Published for every user action (view, search, click). Key = userId. */
    public static final String USER_ACTIVITY = "user.activity";

    /** Published to trigger notification delivery. Key = userId. */
    public static final String NOTIFICATION_DISPATCH = "notification.dispatch";

    // ── Dead Letter Topics ────────────────────────────────────────────────────
    // DLT = Dead Letter Topic. When a consumer fails after max retries,
    // the message is moved here instead of being lost.

    public static final String ORDER_PLACED_DLT = ORDER_PLACED + ".DLT";
    public static final String INVENTORY_UPDATED_DLT = INVENTORY_UPDATED + ".DLT";
    public static final String NOTIFICATION_DISPATCH_DLT = NOTIFICATION_DISPATCH + ".DLT";
}
