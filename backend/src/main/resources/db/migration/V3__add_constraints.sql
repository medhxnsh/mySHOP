-- Phase 2 constraints and indexes
-- Ensuring our tables remain consistent and queries are optimized

-- 1. Unique constraint to ensure a user can only have one cart item per product
-- (This should already exist in cart_items table definition, but making it completely explicit in SQL)
ALTER TABLE cart_items
    DROP CONSTRAINT IF EXISTS uq_cart_product;

ALTER TABLE cart_items
    ADD CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id);

-- 2. Add an index to the orders table by user_id to speed up findByUserIdOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);

-- 3. Add an index to order_items by order_id to speed up the retrieval of items for a specific order
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

-- 4. Add an index to orders by status for the Admin dashboard filtering
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
