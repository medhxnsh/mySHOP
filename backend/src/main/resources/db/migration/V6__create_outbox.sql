-- V6: Transactional Outbox table.
--
-- WHY AN OUTBOX?
-- Before this migration, OrderService published Kafka events INSIDE the
-- @Transactional method — a dual-write: if the DB transaction rolled back
-- after the publish, consumers processed an order that never existed; if the
-- app crashed after commit but before publish, the event was lost forever.
--
-- With the outbox, the event row is INSERTed in the SAME transaction as the
-- business change (atomic: both or neither). A background relay then delivers
-- pending rows to Kafka with at-least-once semantics.

CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64)  NOT NULL,   -- 'ORDER', 'INVENTORY'
    aggregate_id   VARCHAR(64)  NOT NULL,   -- orderId / productId as text
    topic          VARCHAR(128) NOT NULL,   -- value of a KafkaTopics constant
    partition_key  VARCHAR(64)  NOT NULL,   -- preserves per-key ordering semantics
    event_type     VARCHAR(64)  NOT NULL,   -- 'ORDER_PLACED', 'INVENTORY_UPDATED', ...
    payload        JSONB        NOT NULL,   -- JSON-serialized event object
    payload_type   VARCHAR(255) NOT NULL,   -- FQCN used by the relay to rehydrate the
                                            -- payload so Kafka JSON type headers match
                                            -- what consumers already expect
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ,             -- NULL = pending delivery
    attempts       INT          NOT NULL DEFAULT 0
);

-- Partial index: the relay's polling query only ever scans unpublished rows,
-- so the index stays tiny no matter how large the delivered history grows.
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at)
    WHERE published_at IS NULL;
