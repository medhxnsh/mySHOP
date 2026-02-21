-- ============================================================================
-- V2__seed_data.sql — Development seed data
-- Flyway migration file. Version 2 — runs automatically after V1.
--
-- PURPOSE:
-- Provides realistic test data so developers can immediately
-- test API endpoints without manually creating data first.
-- Every endpoint (GET /products, POST /auth/login etc.) works out of the box.
--
-- NEVER EDIT THIS FILE AFTER IT HAS BEEN APPLIED TO ANY DATABASE.
-- To change seed data → create V3__update_seed_data.sql
--
-- WHY ARE PASSWORDS BCRYPT HASHED IN SQL?
-- We can't store plaintext passwords — ever. Even in seed data.
-- BCrypt is a slow one-way hash function designed for passwords:
-- - "Slow" is intentional → brute force attacks take years
-- - The salt is embedded in the hash (the $2a$10$... prefix)
-- - $2a = BCrypt algorithm, $10 = cost factor (2^10 = 1024 rounds)
-- Spring Security's BCryptPasswordEncoder uses cost=10 by default.
--
-- HOW TO VERIFY THESE HASHES ARE CORRECT:
-- import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
-- new BCryptPasswordEncoder().matches("Admin@123", hash) → true
--
-- TEST CREDENTIALS:
-- Admin: admin@myshop.com / Admin@123
-- User:  user@myshop.com  / User@123
-- ============================================================================


-- ── USERS (seed) ─────────────────────────────────────────────────────────────

-- WHY INSERT ... ON CONFLICT DO NOTHING?
-- Flyway is idempotent — it tracks which migrations ran via the flyway_schema_history
-- table. But if somehow the migration is re-applied (edge case in CI), we don't
-- want unique constraint violations to crash the app. This makes the insert safe.

INSERT INTO users (id, email, password_hash, full_name, role, is_active)
VALUES
  -- Admin user — full access to all ADMIN-only endpoints
  -- BCrypt hash of: Admin@123 (cost=10)
  (
    gen_random_uuid(),
    'admin@myshop.com',
    '$2a$10$IeEagXtGDliF/NXGy6.RVOm/pZLlFGsiF0QZJGXjZJ6jA57noxRSa',
    'Shop Admin',
    'ADMIN',
    true
  ),
  -- Regular user — standard customer access
  -- BCrypt hash of: User@123 (cost=10)
  (
    gen_random_uuid(),
    'user@myshop.com',
    '$2a$10$FdMjKZEQu5aOSobjsnrVrOo/4uEvBlNur8clFGMdCRwXU/v9wewF.',
    'Test User',
    'USER',
    true
  )
ON CONFLICT (email) DO NOTHING;


-- ── CATEGORIES (seed) ────────────────────────────────────────────────────────
-- 5 root categories — no parent (flat hierarchy for Phase 0).
-- Parent-child relationships will be added in Phase 1.
--
-- WHY EXPLICIT UUIDs HERE?
-- We need to reference category IDs when inserting products below.
-- gen_random_uuid() makes it impossible to reference later in the same script.
-- Using hardcoded UUIDs (generated once via: SELECT gen_random_uuid()) is the
-- standard approach for seed data with FK relationships.

INSERT INTO categories (id, name, slug)
VALUES
  ('a1b2c3d4-0001-0001-0001-000000000001', 'Electronics',     'electronics'),
  ('a1b2c3d4-0001-0001-0001-000000000002', 'Clothing',        'clothing'),
  ('a1b2c3d4-0001-0001-0001-000000000003', 'Home & Kitchen',  'home-kitchen'),
  ('a1b2c3d4-0001-0001-0001-000000000004', 'Books',           'books'),
  ('a1b2c3d4-0001-0001-0001-000000000005', 'Sports & Fitness','sports-fitness')
ON CONFLICT (slug) DO NOTHING;


-- ── PRODUCTS (seed) ──────────────────────────────────────────────────────────
-- 20 products — 4 per category, varied prices.
-- All are active and in stock so they show up in default product listings.
-- avg_rating and review_count start at 0 — updated by MongoDB reviews in Phase 3.
--
-- PRICE STRATEGY: mix of budget (₹499), mid-range (₹1999-9999), premium (₹24999+)
-- Reflects realistic e-commerce spread for testing price filtering.

INSERT INTO products (id, name, description, price, stock_quantity, sku, avg_rating, review_count, category_id, is_active)
VALUES

  -- ── Electronics (4 products) ───────────────────────────────────────────────
  (
    gen_random_uuid(),
    'Wireless Noise-Cancelling Headphones',
    'Premium over-ear headphones with 30-hour battery life, active noise cancellation, and hi-fi audio. Perfect for remote work and travel.',
    8999.00, 50,
    'ELEC-HEADPHONES-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true
  ),
  (
    gen_random_uuid(),
    'Mechanical Gaming Keyboard',
    'Full-size RGB mechanical keyboard with Cherry MX Red switches. Anti-ghosting, detachable USB-C cable, aluminium chassis.',
    5499.00, 75,
    'ELEC-KEYBOARD-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true
  ),
  (
    gen_random_uuid(),
    '4K USB-C Monitor 27"',
    '27-inch IPS display, 3840×2160 resolution, 99% sRGB colour accuracy, USB-C 65W power delivery. Ideal for designers and developers.',
    24999.00, 30,
    'ELEC-MONITOR-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true
  ),
  (
    gen_random_uuid(),
    'Portable Bluetooth Speaker',
    'IP67 waterproof, 360-degree surround sound, 12-hour battery, USB-C charging. Great for outdoor use.',
    2999.00, 120,
    'ELEC-SPEAKER-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true
  ),

  -- ── Clothing (4 products) ─────────────────────────────────────────────────
  (
    gen_random_uuid(),
    'Men''s Premium Cotton T-Shirt',
    '100% organic cotton, pre-shrunk, crew neck. Available in 12 colours. Sustainably sourced.',
    799.00, 200,
    'CLTH-TSHIRT-M-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true
  ),
  (
    gen_random_uuid(),
    'Women''s Running Jacket',
    'Lightweight, wind-resistant, with reflective stripes for night runs. Zipped pockets. Machine washable.',
    2499.00, 80,
    'CLTH-JACKET-W-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true
  ),
  (
    gen_random_uuid(),
    'Unisex Slim-Fit Chinos',
    'Stretch cotton blend, straight cut, available in khaki, navy, and olive. Office and casual wear.',
    1499.00, 150,
    'CLTH-CHINOS-U-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true
  ),
  (
    gen_random_uuid(),
    'Kids Hooded Sweatshirt',
    'Soft fleece lining, kangaroo pocket, ribbed cuffs. Ages 4-12. Machine washable.',
    999.00, 300,
    'CLTH-HOODIE-K-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true
  ),

  -- ── Home & Kitchen (4 products) ───────────────────────────────────────────
  (
    gen_random_uuid(),
    'Stainless Steel Pressure Cooker 5L',
    'ISI certified, induction-compatible base, auto-locking lid with safety valve. 5-year warranty.',
    1799.00, 60,
    'HOME-COOKER-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000003',
    true
  ),
  (
    gen_random_uuid(),
    'Non-Stick Cookware Set (5 Pieces)',
    'Granite-coated aluminium, PFOA-free, dishwasher safe, suitable for all hob types including induction.',
    3499.00, 40,
    'HOME-COOKWARE-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000003',
    true
  ),
  (
    gen_random_uuid(),
    'Robot Vacuum Cleaner',
    'Smart mapping, auto-docking, 90-minute run time, HEPA filter. Works with Alexa and Google Home.',
    12999.00, 25,
    'HOME-VACUUM-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000003',
    true
  ),
  (
    gen_random_uuid(),
    'Bamboo Cutting Board Set (3 pieces)',
    'Large, medium, and small boards with juice groove. Antibacterial, eco-friendly, dishwasher safe.',
    499.00, 500,
    'HOME-CUTTBOARD-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000003',
    true
  ),

  -- ── Books (4 products) ────────────────────────────────────────────────────
  (
    gen_random_uuid(),
    'Clean Code by Robert C. Martin',
    'A handbook of agile software craftsmanship. Essential reading for any serious developer. Covers naming, functions, classes, and TDD.',
    699.00, 200,
    'BOOK-CLEANCODE-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000004',
    true
  ),
  (
    gen_random_uuid(),
    'Designing Data-Intensive Applications',
    'The definitive guide to distributed systems, databases, and data pipelines by Martin Kleppmann. Must-read for system design interviews.',
    1099.00, 150,
    'BOOK-DDIA-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000004',
    true
  ),
  (
    gen_random_uuid(),
    'The Pragmatic Programmer',
    '20th anniversary edition. From journeyman to master. Timeless advice on software craftsmanship, career, and tools.',
    799.00, 180,
    'BOOK-PRAGPROG-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000004',
    true
  ),
  (
    gen_random_uuid(),
    'System Design Interview (Vol. 2)',
    'Step-by-step guide to cracking system design interviews. Covers rate limiting, CDN, distributed cache, and more.',
    899.00, 250,
    'BOOK-SYSDESIGN-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000004',
    true
  ),

  -- ── Sports & Fitness (4 products) ─────────────────────────────────────────
  (
    gen_random_uuid(),
    'Adjustable Dumbbell Set 2.5–25 kg',
    'Quick-change dial system, replaces 9 pairs of dumbbells. Rubberised grip, compact footprint. Ideal for home gym.',
    9999.00, 35,
    'SPORT-DUMBBELL-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000005',
    true
  ),
  (
    gen_random_uuid(),
    'Yoga Mat — 6mm Extra Thick',
    'Non-slip textured surface, moisture-resistant, includes carrying strap. 183 cm × 61 cm. Available in 6 colours.',
    999.00, 400,
    'SPORT-YOGAMAT-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000005',
    true
  ),
  (
    gen_random_uuid(),
    'Resistance Bands Set (5 levels)',
    'Natural latex, 10–50 lbs resistance levels, anti-snap guarantee. Includes carry bag and door anchor.',
    699.00, 300,
    'SPORT-BANDS-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000005',
    true
  ),
  (
    gen_random_uuid(),
    'Smart Fitness Tracker',
    'Heart rate monitor, sleep tracking, SpO2 sensor, 7-day battery, 5ATM waterproof, iOS and Android compatible.',
    4999.00, 90,
    'SPORT-TRACKER-001',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000005',
    true
  );
