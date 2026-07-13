-- V9: Flash-sale engine (Phase 9).
--
-- DESIGN: the purchase hot path never touches these tables — Redis (a Lua
-- script) is the source of truth for stock and per-user dedup during the
-- sale. This schema is the DURABLE side, written asynchronously:
--   flash_sales:             sale definition + lifecycle (admin-managed)
--   flash_sale_reservations: one row per accepted purchase, written by
--                            FlashOrderWorker when it turns the Kafka
--                            reservation event into a real order.
--
-- UNIQUE(sale_id, user_id) is the worker's idempotency guard: Kafka delivers
-- at-least-once, and a duplicate insert conflicts instead of double-ordering.

CREATE TABLE flash_sales (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id   UUID NOT NULL REFERENCES products (id),
    sale_price   NUMERIC(10, 2) NOT NULL CHECK (sale_price > 0),
    total_stock  INT NOT NULL CHECK (total_stock > 0),
    starts_at    TIMESTAMPTZ NOT NULL,
    ends_at      TIMESTAMPTZ NOT NULL,
    -- DRAFT -> ACTIVE -> ENDED (activation validates + reserves product stock)
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at)
);

CREATE TABLE flash_sale_reservations (
    id          UUID PRIMARY KEY,             -- generated on the hot path, carried in the event
    sale_id     UUID NOT NULL REFERENCES flash_sales (id),
    user_id     UUID NOT NULL REFERENCES users (id),
    status      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED | FAILED
    order_id    UUID REFERENCES orders (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_flash_reservation_user UNIQUE (sale_id, user_id)
);

CREATE INDEX idx_flash_sales_status ON flash_sales (status);
CREATE INDEX idx_flash_reservations_sale ON flash_sale_reservations (sale_id);
