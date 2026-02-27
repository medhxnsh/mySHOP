-- ============================================================================
-- V2__seed_data.sql — Development seed data
-- Flyway migration file. Version 2 — runs automatically after V1.
--
-- PURPOSE:
-- Provides realistic test data so developers can immediately
-- test API endpoints without manually creating data first.
-- Every endpoint (GET /products, POST /auth/login etc.) works out of the box.
--
-- TEST CREDENTIALS:
-- Admin: admin@myshop.com / Admin@123
-- User:  user@myshop.com  / User@123
-- ============================================================================


-- ── USERS (seed) ─────────────────────────────────────────────────────────────

INSERT INTO users (id, email, password_hash, full_name, role, is_active)
VALUES
  -- Admin user — BCrypt hash of: Admin@123 (cost=10)
  (
    gen_random_uuid(),
    'admin@myshop.com',
    '$2a$10$IeEagXtGDliF/NXGy6.RVOm/pZLlFGsiF0QZJGXjZJ6jA57noxRSa',
    'Shop Admin',
    'ADMIN',
    true
  ),
  -- Regular user — BCrypt hash of: User@123 (cost=10)
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

INSERT INTO categories (id, name, slug)
VALUES
  ('a1b2c3d4-0001-0001-0001-000000000001', 'Electronics',     'electronics'),
  ('a1b2c3d4-0001-0001-0001-000000000002', 'Clothing',        'clothing'),
  ('a1b2c3d4-0001-0001-0001-000000000003', 'Home & Kitchen',  'home-kitchen'),
  ('a1b2c3d4-0001-0001-0001-000000000004', 'Books',           'books'),
  ('a1b2c3d4-0001-0001-0001-000000000005', 'Sports & Fitness','sports-fitness'),
  ('a1b2c3d4-0001-0001-0001-000000000006', 'Accessories',     'accessories')
ON CONFLICT (slug) DO NOTHING;


-- ── PRODUCTS (seed) ──────────────────────────────────────────────────────────
-- 20 products sourced from FakeStoreAPI with real images, descriptions, prices.
-- Categories mapped: electronics→Electronics, men's/women's clothing→Clothing, jewelery→Accessories.

INSERT INTO products (id, name, description, price, stock_quantity, sku, avg_rating, review_count, category_id, is_active, image_url)
VALUES

  -- ── Men's Clothing → Clothing ──────────────────────────────────────────────
  (
    gen_random_uuid(),
    'Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops',
    'Your perfect pack for everyday use and walks in the forest. Stash your laptop (up to 15 inches) in the padded sleeve, your everyday',
    109.95, 50,
    'FAKESTORE-1',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_t.png'
  ),
  (
    gen_random_uuid(),
    'Mens Casual Premium Slim Fit T-Shirts',
    'Slim-fitting style, contrast raglan long sleeve, three-button henley placket, light weight & soft fabric for breathable and comfortable wearing. And Solid stitched shirts with round neck made for durability and a great fit for casual fashion wear and diehard baseball fans.',
    22.30, 75,
    'FAKESTORE-2',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/71-3HjGNDUL._AC_SY879._SX._UX._SY._UY_t.png'
  ),
  (
    gen_random_uuid(),
    'Mens Cotton Jacket',
    'great outerwear jackets for Spring/Autumn/Winter, suitable for many occasions, such as working, hiking, camping, mountain/rock climbing, cycling, traveling or other outdoors. Good gift choice for you or your family member.',
    55.99, 60,
    'FAKESTORE-3',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/71li-ujtlUL._AC_UX679_t.png'
  ),
  (
    gen_random_uuid(),
    'Mens Casual Slim Fit',
    'The color could be slightly different between on the screen and in practice. Please note that body builds vary by person, therefore, detailed size information should be reviewed below on the product description.',
    15.99, 100,
    'FAKESTORE-4',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/71YXzeOuslL._AC_UY879_t.png'
  ),

  -- ── Jewelery → Accessories ─────────────────────────────────────────────────
  (
    gen_random_uuid(),
    'John Hardy Women''s Legends Naga Gold & Silver Dragon Station Chain Bracelet',
    'From our Legends Collection, the Naga was inspired by the mythical water dragon that protects the ocean''s pearl. Wear facing inward to be bestowed with love and abundance, or outward for protection.',
    695.00, 30,
    'FAKESTORE-5',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000006',
    true,
    'https://fakestoreapi.com/img/71pWzhdJNwL._AC_UL640_QL65_ML3_t.png'
  ),
  (
    gen_random_uuid(),
    'Solid Gold Petite Micropave',
    'Satisfaction Guaranteed. Return or exchange any order within 30 days. Designed and sold by Hafeez Center in the United States.',
    168.00, 40,
    'FAKESTORE-6',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000006',
    true,
    'https://fakestoreapi.com/img/61sbMiUnoGL._AC_UL640_QL65_ML3_t.png'
  ),
  (
    gen_random_uuid(),
    'White Gold Plated Princess',
    'Classic Created Wedding Engagement Solitaire Diamond Promise Ring for Her. Gifts to spoil your love more for Engagement, Wedding, Anniversary, Valentine''s Day.',
    9.99, 80,
    'FAKESTORE-7',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000006',
    true,
    'https://fakestoreapi.com/img/71YAIFU48IL._AC_UL640_QL65_ML3_t.png'
  ),
  (
    gen_random_uuid(),
    'Pierced Owl Rose Gold Plated Stainless Steel Double',
    'Rose Gold Plated Double Flared Tunnel Plug Earrings. Made of 316L Stainless Steel.',
    10.99, 90,
    'FAKESTORE-8',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000006',
    true,
    'https://fakestoreapi.com/img/51UDEzMJVpL._AC_UL640_QL65_ML3_t.png'
  ),

  -- ── Electronics ────────────────────────────────────────────────────────────
  (
    gen_random_uuid(),
    'WD 2TB Elements Portable External Hard Drive - USB 3.0',
    'USB 3.0 and USB 2.0 Compatibility Fast data transfers Improve PC Performance High Capacity; Formatted NTFS for Windows 10, Windows 8.1, Windows 7.',
    64.00, 55,
    'FAKESTORE-9',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true,
    'https://fakestoreapi.com/img/61IBBVJvSDL._AC_SY879_t.png'
  ),
  (
    gen_random_uuid(),
    'SanDisk SSD PLUS 1TB Internal SSD - SATA III 6 Gb/s',
    'Easy upgrade for faster boot up, shutdown, application load and response. Boosts burst write performance, making it ideal for typical PC workloads. Read/write speeds of up to 535MB/s/450MB/s.',
    109.00, 70,
    'FAKESTORE-10',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true,
    'https://fakestoreapi.com/img/61U7T1koQqL._AC_SX679_t.png'
  ),
  (
    gen_random_uuid(),
    'Silicon Power 256GB SSD 3D NAND A55 SLC Cache Performance Boost SATA III 2.5',
    '3D NAND flash are applied to deliver high transfer speeds. Remarkable transfer speeds that enable faster bootup and improved overall system performance. 7mm slim design suitable for Ultrabooks.',
    109.00, 45,
    'FAKESTORE-11',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true,
    'https://fakestoreapi.com/img/71kWymZ+c+L._AC_SX679_t.png'
  ),
  (
    gen_random_uuid(),
    'WD 4TB Gaming Drive Works with Playstation 4 Portable External Hard Drive',
    'Expand your PS4 gaming experience, Play anywhere Fast and easy, setup Sleek design with high capacity, 3-year manufacturer''s limited warranty.',
    114.00, 35,
    'FAKESTORE-12',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true,
    'https://fakestoreapi.com/img/61mtL65D4cL._AC_SX679_t.png'
  ),
  (
    gen_random_uuid(),
    'Acer SB220Q bi 21.5 inches Full HD (1920 x 1080) IPS Ultra-Thin',
    '21.5 inches Full HD widescreen IPS display And Radeon free Sync technology. Zero-frame design, ultra-thin, 4ms response time, IPS panel. 75 hertz refresh rate.',
    599.00, 25,
    'FAKESTORE-13',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true,
    'https://fakestoreapi.com/img/81QpkIctqPL._AC_SX679_t.png'
  ),
  (
    gen_random_uuid(),
    'Samsung 49-Inch CHG90 144Hz Curved Gaming Monitor',
    '49 INCH SUPER ULTRAWIDE 32:9 CURVED GAMING MONITOR with dual 27 inch screen side by side. QUANTUM DOT (QLED) TECHNOLOGY, HDR support. 144HZ HIGH REFRESH RATE and 1ms ultra fast response time.',
    999.99, 15,
    'FAKESTORE-14',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000001',
    true,
    'https://fakestoreapi.com/img/81Zt42ioCgL._AC_SX679_t.png'
  ),

  -- ── Women's Clothing → Clothing ────────────────────────────────────────────
  (
    gen_random_uuid(),
    'BIYLACLESEN Women''s 3-in-1 Snowboard Jacket Winter Coats',
    'Material: 100% Polyester; Detachable Liner Fabric: Warm Fleece. Stand Collar Liner jacket, keep you warm in cold weather. Zippered Pockets. 3 in 1 Detachable Design provide more convenience.',
    56.99, 65,
    'FAKESTORE-15',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/51Y5NI-I5jL._AC_UX679_t.png'
  ),
  (
    gen_random_uuid(),
    'Lock and Love Women''s Removable Hooded Faux Leather Moto Biker Jacket',
    '100% POLYURETHANE(shell) 100% POLYESTER(lining). Faux leather material for style and comfort. 2-For-One Hooded denim style faux leather jacket. HAND WASH ONLY.',
    29.95, 85,
    'FAKESTORE-16',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/81XH0e8fefL._AC_UY879_t.png'
  ),
  (
    gen_random_uuid(),
    'Rain Jacket Women Windbreaker Striped Climbing Raincoats',
    'Lightweight perfet for trip or casual wear. Long sleeve with hooded, adjustable drawstring waist design. Button and zipper front closure raincoat, fully stripes Lined.',
    39.99, 70,
    'FAKESTORE-17',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/71HblAHs5xL._AC_UY879_-2t.png'
  ),
  (
    gen_random_uuid(),
    'MBJ Women''s Solid Short Sleeve Boat Neck V',
    '95% RAYON 5% SPANDEX, Made in USA or Imported, Do Not Bleach, Lightweight fabric with great stretch for comfort, Ribbed on sleeves and neckline.',
    9.85, 110,
    'FAKESTORE-18',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/71z3kpMAYsL._AC_UY879_t.png'
  ),
  (
    gen_random_uuid(),
    'Opna Women''s Short Sleeve Moisture',
    '100% Polyester, Machine wash. Lightweight, roomy and highly breathable with moisture wicking fabric which helps to keep moisture away. Soft Lightweight Fabric with comfortable V-neck collar.',
    7.95, 95,
    'FAKESTORE-19',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/51eg55uWmdL._AC_UX679_t.png'
  ),
  (
    gen_random_uuid(),
    'DANVOUY Womens T Shirt Casual Cotton Short',
    '95%Cotton,5%Spandex. Features: Casual, Short Sleeve, Letter Print, V-Neck, Fashion Tees. Occasion: Casual/Office/Beach/School/Home/Street. Season: Spring, Summer, Autumn, Winter.',
    12.99, 120,
    'FAKESTORE-20',
    0.0, 0,
    'a1b2c3d4-0001-0001-0001-000000000002',
    true,
    'https://fakestoreapi.com/img/61pHAEJ4NML._AC_UX679_t.png'
  );
