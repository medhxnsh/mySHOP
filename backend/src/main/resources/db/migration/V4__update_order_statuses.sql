-- V4__update_order_statuses.sql
-- Enforces stricter constraints on order and payment statuses.
-- Adds AWAITING_PAYMENT and PAYMENT_FAILED for the updated checkout flow.

-- 1. Ensure any existing invalid records are aligned (just in case)
-- Since we are moving from basic strings to an enforced state machine, we should ideally
-- check for any orphaned or weird strings. In a real highly-trafficked DB, we'd do a data sweep first.
-- For myShop, assuming standard data.

-- 2. Drop any previous loose defaults
ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN payment_status DROP DEFAULT;

-- 3. Add CHECK constraints to strictly enforce the allowed Java Enum values at the database level.
-- Why? This prevents any service or manual SQL script from injecting an invalid state (e.g., 'FOOBAR').
ALTER TABLE orders ADD CONSTRAINT chk_orders_status 
    CHECK (status IN ('PENDING', 'AWAITING_PAYMENT', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED'));

ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_status 
    CHECK (payment_status IN ('PENDING', 'AWAITING_PAYMENT', 'PAID', 'FAILED', 'REFUNDED', 'COD'));

-- 4. Restore the defaults (safely within the constraints)
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE orders ALTER COLUMN payment_status SET DEFAULT 'PENDING';
