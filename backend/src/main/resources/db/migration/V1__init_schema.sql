-- ============================================================================
-- V1__init_schema.sql — Initial database schema for myShop
-- Flyway migration file. Version 1 — the foundation everything builds on.
--
-- FLYWAY RULES:
-- 1. File naming: V{version}__{description}.sql (two underscores)
--    version must increase monotonically: V1, V2, V3...
-- 2. NEVER EDIT THIS FILE AFTER IT HAS BEEN APPLIED TO ANY DATABASE.
--    Flyway stores a checksum. If the file changes, next startup will FAIL
--    with "checksum mismatch" — a safety net to prevent unauthorized changes.
-- 3. To change the schema, create V2__your_change.sql instead.
--
-- WHY UUID AS PRIMARY KEY (instead of auto-increment integer)?
-- UUIDs are globally unique across all machines and databases.
-- This means:
-- - You can generate IDs client-side or in application code without asking the DB
-- - Merging data from multiple databases is safe (no ID collision)
-- - IDs don't leak business information (id=1000 tells attackers you have ~1000 users)
-- Tradeoff: UUIDs are 16 bytes vs 4 bytes for int — slightly larger index.
--
-- WHY gen_random_uuid()?
-- PostgreSQL's built-in UUID generator using cryptographically secure randomness.
-- Equivalent to UUID.randomUUID() in Java. RFC 4122 version 4.
-- ============================================================================

-- Enable UUID generation function (comes with PostgreSQL, no extension needed in v14+)
-- gen_random_uuid() is available without pgcrypto in PostgreSQL 13+

-- ── USERS ──────────────────────────────────────────────────────────────────
-- Stores authentication and profile information for all users.
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- UNIQUE constraint creates an index automatically.
    -- Why UNIQUE on email? Prevents duplicate accounts. The index also
    -- speeds up "find user by email" queries (used in every login).
    email VARCHAR(255) UNIQUE NOT NULL,

    -- We store the HASH, never the plaintext password.
    -- BCrypt hashes are always 60 characters. VARCHAR(255) gives room for future
    -- algorithm changes (Argon2 produces longer hashes).
    password_hash VARCHAR(255) NOT NULL,

    full_name VARCHAR(255) NOT NULL,

    -- Role is a string for simplicity. Alternative: a separate roles table
    -- with a join table for many-to-many. We keep it simple because we only
    -- have two roles (USER, ADMIN). If roles grow, migrate to a join table.
    role VARCHAR(20) NOT NULL DEFAULT 'USER',

    -- Soft delete pattern: set is_active=false instead of deleting the row.
    -- Why? Deleting rows causes foreign key problems (orders reference users).
    -- With soft delete, we preserve referential integrity and audit history.
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- All timestamps in UTC (TIMESTAMP without timezone).
    -- WHY not TIMESTAMPTZ (with timezone)? Simpler — we store UTC everywhere,
    -- convert to user's timezone only at the presentation layer.
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);


-- ── CATEGORIES ────────────────────────────────────────────────────────────
-- Hierarchical product categories using the adjacency list pattern.
-- A category can have a parent (subcategory) or no parent (root category).
-- Example: Electronics → Laptops → Gaming Laptops (3 levels deep)
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) NOT NULL,

    -- A slug is a URL-friendly version of the name.
    -- "Gaming Laptops" → "gaming-laptops"
    -- Used in URLs: /products?category=gaming-laptops instead of ?categoryId=uuid
    -- Much more readable and SEO-friendly.
    slug VARCHAR(255) UNIQUE NOT NULL,

    -- Self-referencing foreign key: parent_id points to another category's id.
    -- NULL means this is a root/top-level category.
    -- ON DELETE: If parent is deleted, what happens to children?
    -- We don't set CASCADE here — force explicit handling (business decision).
    parent_id UUID REFERENCES categories(id),

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);


-- ── PRODUCTS ────────────────────────────────────────────────────────────────
-- Core product catalog.
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(500) NOT NULL,
    description TEXT,  -- TEXT in PostgreSQL: unlimited length (up to 1GB)

    -- DECIMAL(10, 2): 10 total digits, 2 decimal places.
    -- Why DECIMAL not FLOAT? Float has rounding errors (0.1 + 0.2 ≠ 0.3 in binary).
    -- DECIMAL is exact — critical for money. Google "floating point money bug".
    price DECIMAL(10, 2) NOT NULL,

    stock_quantity INTEGER NOT NULL DEFAULT 0,

    -- SKU = Stock Keeping Unit. A unique identifier used in warehouses.
    -- Example: "APPLE-IPHONE-15-BLK-128GB"
    sku VARCHAR(100) UNIQUE NOT NULL,

    -- Denormalized rating fields — calculated from MongoDB reviews (Phase 3).
    -- Why store here instead of computing with a MongoDB aggregate on every request?
    -- Performance: one PostgreSQL field read vs cross-database aggregation.
    -- Tradeoff: eventual consistency — rating updates are slightly delayed.
    avg_rating DECIMAL(3, 2) DEFAULT 0.0,
    review_count INTEGER DEFAULT 0,

    category_id UUID REFERENCES categories(id),

    is_active BOOLEAN NOT NULL DEFAULT true,

    -- OPTIMISTIC LOCKING: The @Version field in the JPA entity maps to this column.
    -- How it works:
    -- 1. Thread A reads product, gets version=5
    -- 2. Thread B reads same product, also gets version=5
    -- 3. Thread A updates: UPDATE products SET stock=19, version=6 WHERE id=X AND version=5
    -- 4. Thread B tries to update: UPDATE products SET stock=18, version=6 WHERE id=X AND version=5
    --    → 0 rows affected (version is now 6, not 5) → Hibernate throws OptimisticLockingException
    -- 5. Thread B retries — reads fresh data, sees stock=19, deducts to 18
    -- This prevents overselling without database-level row locking (which is slow).
    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_is_active ON products(is_active);
-- Useful for product listing sorted by price
CREATE INDEX idx_products_price ON products(price);
-- Useful for filtering active products in a category
CREATE INDEX idx_products_category_active ON products(category_id, is_active);


-- ── CARTS ────────────────────────────────────────────────────────────────────
-- One cart per user (UNIQUE constraint on user_id enforces this).
-- Cart is created lazily — only when user first adds an item.
CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- UNIQUE ensures one cart per user (enforced by DB, not just application code)
    user_id UUID UNIQUE REFERENCES users(id),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_carts_user_id ON carts(user_id);


-- ── CART ITEMS ────────────────────────────────────────────────────────────────
-- Items within a cart. UNIQUE(cart_id, product_id) prevents duplicate entries
-- for the same product — instead, update quantity.
CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- ON DELETE CASCADE: when cart is deleted, all its items are deleted too.
    -- This is correct — cart items have no meaning without their cart.
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,

    product_id UUID NOT NULL REFERENCES products(id),

    quantity INTEGER NOT NULL DEFAULT 1,

    -- COMPOSITE UNIQUE: a product can appear only once per cart.
    -- The constraint name (uq_...) is explicit — helpful in error messages.
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);


-- ── ORDERS ────────────────────────────────────────────────────────────────────
-- Represents a completed purchase. Immutable once placed (per design).
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users(id),

    -- Order lifecycle states:
    -- PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → CANCELLED
    -- PENDING: order placed, payment not yet confirmed
    -- CONFIRMED: payment received, ready to fulfill
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    total_amount DECIMAL(10, 2) NOT NULL,

    -- Payment state machine: PENDING → PAID → REFUNDED
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- External payment reference (from payment gateway, or our mock ID)
    payment_reference VARCHAR(255),

    -- JSONB (not JSON): Binary JSON in PostgreSQL.
    -- Why store address as JSONB instead of a separate address table?
    -- The shipping address is a SNAPSHOT of the address AT ORDER TIME.
    -- If the user changes their address later, the order should still show
    -- the address it was shipped to. JSONB snapshot captures this perfectly.
    -- JSONB is also faster to query than JSON (pre-parsed binary format).
    shipping_address JSONB NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
-- Admin query: find orders by status + created date
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC);


-- ── ORDER ITEMS ────────────────────────────────────────────────────────────────
-- Line items within an order. Separate table (not JSONB) because we might
-- query "what products have been sold most" — easier with relational data.
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,

    product_id UUID NOT NULL REFERENCES products(id),

    quantity INTEGER NOT NULL,

    -- Price SNAPSHOT: stores the price at the time of purchase.
    -- Why? Product prices can change. The receipt must show what you paid,
    -- not the current price. This is the snapshot pattern.
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL  -- unit_price * quantity (pre-calculated for performance)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
