# myShop — Antigravity Master Prompt
# E-Commerce Backend Learning Project

---

## CRITICAL RULES — READ BEFORE ANYTHING ELSE

1. **NEVER advance to the next phase automatically.** Stop completely after each phase. Wait for explicit instruction: "proceed to Phase X" or "start next phase."
2. **After completing each phase**, generate a `docs/PHASE_X_SUMMARY.md` with: what was built, why each decision was made, key concepts explained simply, all API endpoints with sample request/response, and 3 MAANG-style interview Q&As about that phase's technology.
3. **Build a working frontend UI for every phase.** The learner must be able to open a browser and test every feature visually.
4. **Every phase must run with a single command:** `docker compose up --build`
5. **Comment the WHY, not just the WHAT.** This learner knows Java basics but is new to Spring. Every annotation, every config class, every design decision needs a comment explaining why it exists.
6. **Never use shortcuts that hide learning.** No `spring.jpa.hibernate.ddl-auto=create`. No skipping Flyway. No putting business logic in controllers.
7. **Follow the project structure defined below from Phase 1.** Never reorganize it mid-project.
8. **On phase completion**, print a summary of: all files created, all Docker services running, all endpoints available, and how to verify everything works.

---

## Learner Context

- **Java level:** Knows Java basics (OOP, collections, generics). New to Spring Boot.
- **Goal:** Crack MAANG SDE1 interviews + strong portfolio project + deep tech understanding + Azure deployment experience.
- **Pace:** 3-4 hours per day. Phases should be thorough, not rushed.
- **Approach:** Explain everything. No magic. If Spring does something automatically, say what it does and why.

---

## Project Overview

**Name:** myShop — A simplified Amazon-style e-commerce platform.

**Why a monolith?** Microservices solve problems that monoliths create. We build the monolith first, feel those problems ourselves, then understand why the industry moved to microservices. This is also more realistic for SDE1 interviews which rarely test microservice implementation.

**Primary Learning Stack:**
| Layer | Technology | Introduced In |
|---|---|---|
| Language | Java 21 | Phase 1 |
| Framework | Spring Boot 3.x | Phase 1 |
| Primary DB | PostgreSQL 16 | Phase 1 |
| Migrations | Flyway | Phase 1 |
| Async/Threading | Java Concurrency + @Async | Phase 1 onwards |
| Document Store | MongoDB 7 | Phase 3 |
| Cache | Redis 7 | Phase 4 |
| Message Broker | Apache Kafka (KRaft — no Zookeeper) | Phase 5 |
| Search | Elasticsearch 8 | Phase 6 |
| Observability | Prometheus + Grafana | Phase 7 |
| Deployment | Docker + Azure | Phase 7 |

**Frontend:** React 18 + Vite + TailwindCSS + Axios + Zustand (state management). Exists purely for testing. Keep it clean but don't over-engineer it.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                   React + Vite (Port 3000)                      │
│              Nginx reverse proxy → /api → backend               │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP/REST (JSON)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Monolith (Port 8080)                   │
│                                                                 │
│  Controllers → Services → Repositories                          │
│                    │                                            │
│              Async Layer (@Async + ThreadPools)                 │
│              CompletableFuture for parallel ops                 │
└────┬──────────┬──────────────┬─────────────────┬───────────────┘
     │          │              │                 │
     ▼          ▼              ▼                 ▼
PostgreSQL   MongoDB        Redis           Kafka (KRaft)
(Relational) (Documents)   (Cache/Lock)    (Events)
                                               │
                                    ┌──────────┴──────────┐
                                    │   Kafka Consumers   │
                                    │ (Notification,      │
                                    │  Analytics, ES Sync)│
                                    └─────────────────────┘
                                               │
                                        Elasticsearch
                                         (Search Index)
```

---

## Database Design (Establish in Phase 1, never change structure)

### PostgreSQL Schema

```sql
-- USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',     -- USER, ADMIN
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- CATEGORIES TABLE (self-referencing for subcategories)
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    parent_id UUID REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- PRODUCTS TABLE
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(500) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    sku VARCHAR(100) UNIQUE NOT NULL,
    avg_rating DECIMAL(3, 2) DEFAULT 0.0,
    review_count INTEGER DEFAULT 0,
    category_id UUID REFERENCES categories(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,            -- Optimistic locking
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- CARTS TABLE
CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES users(id),     -- one cart per user
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- CART_ITEMS TABLE
CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    UNIQUE (cart_id, product_id)
);

-- ORDERS TABLE
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_reference VARCHAR(255),
    shipping_address JSONB NOT NULL,              -- snapshot of address at order time
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ORDER_ITEMS TABLE
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,           -- snapshot of price at order time
    subtotal DECIMAL(10, 2) NOT NULL
);
```

### MongoDB Collections (Added Phase 3)

```javascript
// product_reviews
{
  _id: ObjectId,
  productId: "uuid",        // references PostgreSQL products.id
  userId: "uuid",           // references PostgreSQL users.id
  userName: "string",       // denormalized — avoid join across databases
  rating: 1-5,
  title: "string",
  comment: "string",
  helpfulVotes: 0,
  verifiedPurchase: boolean,
  createdAt: ISODate,
  updatedAt: ISODate
}
// Indexes: { productId: 1 }, { userId: 1 }, { productId: 1, createdAt: -1 }

// user_activity_logs
{
  _id: ObjectId,
  userId: "uuid",
  action: "PRODUCT_VIEWED | PRODUCT_SEARCHED | CART_UPDATED | ORDER_PLACED",
  entityType: "PRODUCT | ORDER | SEARCH",
  entityId: "uuid",
  metadata: { key: "value" },  // flexible
  timestamp: ISODate
}
// TTL index: { timestamp: 1 } expires after 90 days

// notifications
{
  _id: ObjectId,
  userId: "uuid",
  type: "ORDER_CONFIRMED | ORDER_SHIPPED | REVIEW_HELPFUL",
  title: "string",
  body: "string",
  isRead: false,
  metadata: { orderId: "uuid" },
  createdAt: ISODate
}
```

### Redis Key Design (Added Phase 4)

```
product:{id}                          → JSON string    TTL: 10 min
products:list:{hash-of-params}        → JSON string    TTL: 5 min
categories:all                        → JSON string    TTL: 1 hour
rate_limit:{ip}:{minute-window}       → Integer        TTL: 1 min
jwt:refresh:{userId}                  → token string   TTL: 7 days
lock:stock:{productId}                → "1"            TTL: 5 sec
search:suggest:{prefix}               → JSON string    TTL: 30 min
```

### Kafka Topics (Added Phase 5, KRaft Mode — No Zookeeper)

```
order.placed              partitions:3  key:userId
order.status.updated      partitions:3  key:orderId
inventory.updated         partitions:3  key:productId
user.activity             partitions:6  key:userId
notification.dispatch     partitions:3  key:userId

Dead Letter Topics (auto-created):
order.placed.DLT
inventory.updated.DLT
notification.dispatch.DLT
```

### Elasticsearch Index (Added Phase 6)

```json
{
  "index": "products",
  "mappings": {
    "properties": {
      "id":          { "type": "keyword" },
      "name":        { "type": "text", "analyzer": "english",
                       "fields": { "keyword": { "type": "keyword" } } },
      "description": { "type": "text", "analyzer": "english" },
      "category":    { "type": "keyword" },
      "price":       { "type": "double" },
      "avgRating":   { "type": "float" },
      "stockQty":    { "type": "integer" },
      "inStock":     { "type": "boolean" },
      "sku":         { "type": "keyword" }
    }
  },
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 }
}
```

---

## Project Structure (Create in Phase 1, never change)

```
myshop/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/myshop/
│   │   │   │   ├── myShopApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── AsyncConfig.java          # Thread pool bean definitions
│   │   │   │   │   ├── SecurityConfig.java        # Spring Security + JWT filter chain
│   │   │   │   │   ├── OpenApiConfig.java         # Swagger/OpenAPI setup
│   │   │   │   │   ├── CacheConfig.java           # Redis cache manager (Phase 4)
│   │   │   │   │   ├── KafkaConfig.java           # Kafka producer/consumer (Phase 5)
│   │   │   │   │   └── ElasticsearchConfig.java   # ES client (Phase 6)
│   │   │   │   ├── controller/
│   │   │   │   │   └── v1/                        # Always version from day 1
│   │   │   │   │       ├── AuthController.java
│   │   │   │   │       ├── ProductController.java
│   │   │   │   │       ├── CategoryController.java
│   │   │   │   │       ├── CartController.java
│   │   │   │   │       ├── OrderController.java
│   │   │   │   │       ├── ReviewController.java  # Phase 3
│   │   │   │   │       ├── SearchController.java  # Phase 6
│   │   │   │   │       ├── NotificationController.java # Phase 5
│   │   │   │   │       └── AdminController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── ProductService.java
│   │   │   │   │   ├── CategoryService.java
│   │   │   │   │   ├── CartService.java
│   │   │   │   │   ├── OrderService.java
│   │   │   │   │   ├── ReviewService.java         # Phase 3
│   │   │   │   │   ├── CacheService.java          # Phase 4
│   │   │   │   │   ├── SearchService.java         # Phase 6
│   │   │   │   │   ├── NotificationService.java   # Phase 5
│   │   │   │   │   └── ActivityLogService.java    # @Async, Phase 1
│   │   │   │   ├── repository/
│   │   │   │   │   ├── jpa/                       # PostgreSQL repos
│   │   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   │   ├── ProductRepository.java
│   │   │   │   │   │   ├── CategoryRepository.java
│   │   │   │   │   │   ├── CartRepository.java
│   │   │   │   │   │   ├── CartItemRepository.java
│   │   │   │   │   │   ├── OrderRepository.java
│   │   │   │   │   │   └── OrderItemRepository.java
│   │   │   │   │   ├── mongo/                     # MongoDB repos (Phase 3)
│   │   │   │   │   │   ├── ReviewRepository.java
│   │   │   │   │   │   ├── ActivityLogRepository.java
│   │   │   │   │   │   └── NotificationRepository.java
│   │   │   │   │   └── search/                    # ES repos (Phase 6)
│   │   │   │   │       └── ProductSearchRepository.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── entity/                    # JPA @Entity classes
│   │   │   │   │   │   ├── User.java
│   │   │   │   │   │   ├── Product.java
│   │   │   │   │   │   ├── Category.java
│   │   │   │   │   │   ├── Cart.java
│   │   │   │   │   │   ├── CartItem.java
│   │   │   │   │   │   ├── Order.java
│   │   │   │   │   │   └── OrderItem.java
│   │   │   │   │   ├── document/                  # MongoDB @Document (Phase 3)
│   │   │   │   │   │   ├── ProductReview.java
│   │   │   │   │   │   ├── UserActivityLog.java
│   │   │   │   │   │   └── Notification.java
│   │   │   │   │   └── search/                    # ES @Document (Phase 6)
│   │   │   │   │       └── ProductDocument.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/                   # Incoming request bodies
│   │   │   │   │   └── response/                  # Outgoing response bodies
│   │   │   │   ├── mapper/                        # MapStruct mappers
│   │   │   │   │   ├── ProductMapper.java
│   │   │   │   │   ├── UserMapper.java
│   │   │   │   │   └── OrderMapper.java
│   │   │   │   ├── event/
│   │   │   │   │   ├── kafka/                     # Kafka event POJOs
│   │   │   │   │   │   ├── OrderPlacedEvent.java
│   │   │   │   │   │   ├── InventoryUpdatedEvent.java
│   │   │   │   │   │   └── NotificationEvent.java
│   │   │   │   │   └── internal/                  # Spring ApplicationEvents
│   │   │   │   │       └── OrderCreatedEvent.java
│   │   │   │   ├── kafka/                         # Phase 5
│   │   │   │   │   ├── producer/
│   │   │   │   │   │   ├── OrderEventProducer.java
│   │   │   │   │   │   └── InventoryEventProducer.java
│   │   │   │   │   └── consumer/
│   │   │   │   │       ├── NotificationConsumer.java
│   │   │   │   │       ├── AnalyticsConsumer.java
│   │   │   │   │       └── InventorySyncConsumer.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   ├── InsufficientStockException.java
│   │   │   │   │   └── ErrorCode.java             # All error codes as enum
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │   ├── util/
│   │   │   │   │   ├── RequestIdFilter.java       # Correlation ID for every request
│   │   │   │   │   └── SecurityUtils.java
│   │   │   │   └── constants/
│   │   │   │       ├── CacheKeys.java             # Redis key constants (no magic strings)
│   │   │   │       ├── KafkaTopics.java           # Topic name constants
│   │   │   │       └── AppConstants.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/migration/                  # Flyway: V1__init.sql, V2__...
│   │   └── test/
│   │       ├── java/com/myshop/
│   │       │   ├── unit/                          # Mockito unit tests
│   │       │   └── integration/                   # Testcontainers integration tests
│   │       └── resources/
│   │           └── application-test.yml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   ├── components/
│   │   ├── services/                              # Axios API functions
│   │   ├── store/                                 # Zustand state
│   │   ├── hooks/
│   │   └── utils/
│   ├── nginx.conf                                 # Reverse proxy to backend
│   ├── Dockerfile
│   └── package.json
├── docker/
│   ├── kafka/                                     # KRaft config
│   │   └── kraft.properties
│   └── grafana/
│       └── dashboards/
│           └── myshop.json
├── docker-compose.yml
├── docker-compose.override.yml                    # Dev port overrides
├── .env.example
├── .github/
│   └── workflows/
│       └── ci.yml
└── docs/
    ├── ARCHITECTURE.md
    ├── PHASE_1_SUMMARY.md                         # Generated after each phase
    └── ...
```

---

## Multithreading Plan (Integrated Across All Phases)

This is not an add-on — it runs through the whole project because real applications are always concurrent.

### Phase 1 — Introduce @Async and Custom ThreadPools
Create `AsyncConfig.java` with two named thread pools:

```java
// General async tasks (email simulation, logging)
@Bean("generalTaskExecutor")
ThreadPoolTaskExecutor with: corePoolSize=2, maxPoolSize=5, queueCapacity=100
ThreadNamePrefix: "async-general-"
RejectedExecutionHandler: CallerRunsPolicy (fallback: run in caller's thread)

// Analytics/logging tasks (high volume, non-critical)
@Bean("analyticsTaskExecutor")
ThreadPoolTaskExecutor with: corePoolSize=4, maxPoolSize=10, queueCapacity=500
ThreadNamePrefix: "async-analytics-"
```

Use `@Async("analyticsTaskExecutor")` on `ActivityLogService.logActivity()` — every product view and search is logged without slowing down the HTTP response.

### Phase 2 — CompletableFuture for Parallel Post-Order Processing
When an order is placed, three things must happen. Only #1 is critical:
```
1. Deduct stock from PostgreSQL         ← SYNC (must succeed before returning)
2. Send confirmation "email" (log it)  ← ASYNC via CompletableFuture
3. Log analytics event                  ← ASYNC via CompletableFuture

CompletableFuture.runAsync(() -> sendEmail(order), emailExecutor);
CompletableFuture.runAsync(() -> logEvent(order), analyticsExecutor);
// Don't wait for these — return the order response immediately
```

### Phase 4 — Parallel Cache Warming on Startup
On application start, pre-warm Redis with the top 100 products:
```java
// Split into batches of 10, process in parallel
List<CompletableFuture<Void>> futures = batches.stream()
    .map(batch -> CompletableFuture.runAsync(() -> warmBatch(batch), cacheExecutor))
    .collect(toList());
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

### Phase 5 — Kafka Consumer Concurrency
```java
@KafkaListener(topics = "order.placed", concurrency = "3")
// concurrency=3 means 3 consumer threads, each handling one partition
// Manual acknowledgment: message not marked consumed until processing succeeds
```

### Phase 6 — CountDownLatch for Bulk Reindex
```java
// Reindex all products into Elasticsearch in parallel batches
CountDownLatch latch = new CountDownLatch(batches.size());
AtomicInteger successCount = new AtomicInteger(0);

for (List<Product> batch : batches) {
    executor.submit(() -> {
        try {
            indexBatch(batch);
            successCount.addAndGet(batch.size());
        } finally {
            latch.countDown(); // always countdown even on failure
        }
    });
}
latch.await(5, TimeUnit.MINUTES);
```

### Phase 7 — Java 21 Virtual Threads
```java
// Replace traditional thread pool for I/O-bound tasks
// In application.yml:
spring.threads.virtual.enabled: true
// This makes Spring use virtual threads for every HTTP request handler
// Explain: Virtual threads are cheap — millions can exist vs thousands of platform threads
```

### Threading Concepts Covered (Interview Topics)
- `@Async` and why you need a custom Executor (never use default ForkJoinPool for I/O)
- `CompletableFuture` — chaining, `thenApply`, `thenAccept`, `allOf`, `anyOf`
- Thread pool sizing: CPU-bound `(N cores + 1)` vs I/O-bound `(N cores * 2)` rule
- `CallerRunsPolicy` as backpressure mechanism
- Race conditions — demonstrate with stock deduction (show bug, then fix with lock)
- `AtomicInteger` / `AtomicLong` — lock-free counters
- `CountDownLatch` — waiting for parallel tasks
- Virtual threads (Project Loom, Java 21) — why they exist, how they differ

---

## API Design

### Versioning
All endpoints: `/api/v1/` — never change this, never break existing endpoints.

### Authentication
- JWT Access Token: 15-minute TTL, sent in `Authorization: Bearer {token}` header
- JWT Refresh Token: 7-day TTL, stored in Redis, returned in `httpOnly` cookie
- Public endpoints: `POST /api/v1/auth/*`, `GET /api/v1/products`, `GET /api/v1/search`
- Admin-only endpoints: annotated with `@PreAuthorize("hasRole('ADMIN')")`

### Standard Response Envelope (all controllers return this)
```json
{
  "success": true,
  "data": {},
  "message": "OK",
  "timestamp": "2024-01-01T10:00:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Error Response Envelope
```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "Product with id 'abc-123' was not found.",
    "details": []
  },
  "timestamp": "2024-01-01T10:00:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Complete API Surface

```
AUTH
  POST   /api/v1/auth/register            body: {email, password, fullName}
  POST   /api/v1/auth/login               body: {email, password}
  POST   /api/v1/auth/refresh             cookie: refreshToken
  POST   /api/v1/auth/logout

USERS
  GET    /api/v1/users/me
  PUT    /api/v1/users/me
  PUT    /api/v1/users/me/password

CATEGORIES
  GET    /api/v1/categories
  POST   /api/v1/categories               [ADMIN]
  PUT    /api/v1/categories/{id}          [ADMIN]

PRODUCTS
  GET    /api/v1/products                 ?page=0&size=20&category=&sort=price,asc&minPrice=&maxPrice=
  GET    /api/v1/products/{id}
  POST   /api/v1/products                 [ADMIN]
  PUT    /api/v1/products/{id}            [ADMIN]
  DELETE /api/v1/products/{id}            [ADMIN]
  PUT    /api/v1/products/{id}/stock      [ADMIN] body: {quantity}

CART
  GET    /api/v1/cart
  POST   /api/v1/cart/items               body: {productId, quantity}
  PUT    /api/v1/cart/items/{productId}   body: {quantity}
  DELETE /api/v1/cart/items/{productId}
  DELETE /api/v1/cart

ORDERS
  POST   /api/v1/orders                   body: {shippingAddress}
  GET    /api/v1/orders                   my orders, paginated
  GET    /api/v1/orders/{id}
  PUT    /api/v1/orders/{id}/cancel
  GET    /api/v1/admin/orders             [ADMIN] ?status=&page=
  PUT    /api/v1/admin/orders/{id}/status [ADMIN] body: {status}

PAYMENT (Simulated)
  POST   /api/v1/orders/{id}/pay          body: {paymentMethod: "MOCK"}
  GET    /api/v1/orders/{id}/payment

REVIEWS (Phase 3)
  POST   /api/v1/products/{id}/reviews    body: {rating, title, comment}
  GET    /api/v1/products/{id}/reviews    ?page=&sort=recent|helpful
  PUT    /api/v1/reviews/{id}/helpful
  DELETE /api/v1/reviews/{id}

NOTIFICATIONS (Phase 5)
  GET    /api/v1/notifications            my unread notifications
  PUT    /api/v1/notifications/{id}/read
  PUT    /api/v1/notifications/read-all

SEARCH (Phase 6)
  GET    /api/v1/search                   ?q=&category=&minPrice=&maxPrice=&sort=&page=
  GET    /api/v1/search/suggest           ?q=

ADMIN — OPERATIONS
  POST   /api/v1/admin/search/reindex     [ADMIN] rebuild ES index
  GET    /api/v1/admin/cache/stats        [ADMIN]
  DELETE /api/v1/admin/cache              [ADMIN] ?key=
  GET    /api/v1/admin/analytics/summary  [ADMIN]
```

---

## Phase Breakdown

---

### PHASE 0 — Project Scaffold (Do This Phase First)

**Goal:** Create the skeleton. Everything compiles. Docker runs. No features yet.

**Tasks:**
1. Initialize Spring Boot project with these dependencies: `spring-web`, `spring-data-jpa`, `postgresql`, `flyway-core`, `spring-security`, `spring-boot-starter-validation`, `spring-boot-actuator`, `lombok`, `mapstruct`, `jjwt`, `springdoc-openapi-starter-webmvc-ui`
2. Create the full folder structure as defined above (empty files with package declarations are fine)
3. Create `application.yml` with all configuration placeholders
4. Create `docker-compose.yml` with only `postgres` and `backend` and `frontend` services
5. Create `.env.example` with all required environment variables
6. Create `.gitignore` exactly as defined in the Git Configuration section — `docs/` must be listed so phase reports never appear in git history
6. Create `RequestIdFilter.java` — intercepts every request, adds `X-Request-ID` UUID to MDC for logging, adds it to response headers
7. Create `GlobalExceptionHandler.java` with handlers for: `ResourceNotFoundException`, `BusinessException`, `MethodArgumentNotValidException`, `AccessDeniedException`, generic `Exception`
8. Create `ApiResponse<T>` wrapper class
9. Create `ErrorCode` enum with all error codes the app will ever use
10. Initialize React + Vite frontend with TailwindCSS and Axios installed. Create a "Coming Soon" landing page.
11. Flyway: Create `V1__init_schema.sql` with the full PostgreSQL schema defined above.
12. Verify: `docker compose up --build` starts everything without errors. Actuator health endpoint responds at `http://localhost:8080/actuator/health`.

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 1 — Core: Products, Users, JWT Auth

**Goal:** Working product catalog with auth. Learn Spring fundamentals deeply.

**Concepts Introduced:**
- Spring Boot auto-configuration (what happens at startup)
- Dependency injection and the Spring container
- JPA entities, repositories, and how Hibernate generates SQL
- Why DTOs exist (never expose entities directly)
- MapStruct for compile-time mapping
- Flyway for versioned migrations
- Spring Security filter chain
- JWT — generation, validation, stateless auth
- `@Async` — non-blocking activity logging
- Bean Validation with `@Valid`
- Spring `@Transactional` basics
- Pagination with `Pageable`

**Backend Tasks:**
1. Implement `User` entity with JPA annotations. Explain each annotation in comments.
2. Implement `Product` and `Category` entities. Include `@Version` on Product with a comment explaining optimistic locking.
3. Implement all JPA repositories. Show examples of: `findByEmail`, `findBySlug`, custom `@Query` for product search with price filter.
4. Implement all DTOs as Java records where possible (Java 16+). Explain why records for DTOs.
5. Implement MapStruct mappers. Explain what MapStruct does at compile time.
6. Implement `AuthService` — register (hash password with BCrypt), login (validate + generate JWT), refresh token logic.
7. Implement `JwtTokenProvider` with sign, validate, extract claims. Use `HS256` algorithm. Comment every step.
8. Implement `JwtAuthenticationFilter` — explain how it plugs into the Spring Security filter chain.
9. Implement `SecurityConfig` — explain `SecurityFilterChain`, `csrf().disable()` (why for REST APIs), `sessionManagement(STATELESS)`.
10. Implement `ProductService` with: create, update, delete, findById, findAll (paginated with filters).
11. Implement `ActivityLogService` with `@Async("analyticsTaskExecutor")` — logs user actions to console for now (MongoDB in Phase 3).
12. Call `activityLogService.log(...)` from product view endpoint. Show that the HTTP response returns immediately without waiting for logging.
13. Implement `AsyncConfig.java` — define the two thread pool beans. Add comments explaining each parameter: corePoolSize, maxPoolSize, queueCapacity, rejectedExecutionHandler.
14. All controllers: `AuthController`, `ProductController`, `CategoryController`.
15. Seed data: Flyway `V2__seed_data.sql` — 5 categories, 20 products, 1 admin user, 1 regular user.
16. OpenAPI: accessible at `http://localhost:8080/swagger-ui.html`. All endpoints documented.

**Frontend Tasks:**
- Home page: grid of products, paginated, category filter sidebar
- Product detail page: name, price, description, stock status, "Add to Cart" button (disabled for now)
- Login/Register pages with form validation
- JWT stored in memory (not localStorage — explain XSS risk in a comment)
- Axios interceptor that adds `Authorization` header to every request
- Admin nav item visible only when user role is ADMIN
- Admin product management page: list, add, edit, delete products

**Testing:**
- Unit test: `ProductServiceTest` — test `createProduct`, `updateProduct`, mock repository
- Integration test: `ProductControllerIntegrationTest` using Testcontainers PostgreSQL — test the full HTTP → DB roundtrip

**After completion:** Generate `docs/PHASE_1_SUMMARY.md`

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 2 — Orders, Transactions, and CompletableFuture

**Goal:** Implement the full shopping flow. Learn transactions, concurrent operations, and payment states.

**Concepts Introduced:**
- `@Transactional` propagation levels — `REQUIRED`, `REQUIRES_NEW`
- Optimistic locking in action — demonstrate the `ObjectOptimisticLockingFailureException`
- N+1 query problem — show it, then fix with `JOIN FETCH`
- `CompletableFuture` — parallel async operations after order placement
- Payment state machine — `PENDING → PAID → REFUNDED`
- Spring `ApplicationEvent` — internal order placed event
- Idempotency concept — what happens if the same request fires twice

**Backend Tasks:**
1. Implement `Cart`, `CartItem`, `Order`, `OrderItem` entities.
2. Implement `CartService`: add item (check stock), update quantity, remove item, get cart with total.
3. Implement `OrderService.placeOrder()`:
   - Validate cart is not empty
   - Lock stock with optimistic locking (`@Version`)
   - Deduct stock from each product in one transaction
   - Create Order and OrderItems with price snapshot
   - Clear the cart
   - Publish internal `OrderCreatedEvent` via Spring `ApplicationEventPublisher`
   - After returning, fire two `CompletableFuture` tasks: simulate email (log), log analytics
   - If stock deduction fails mid-order, entire transaction rolls back — demonstrate this
4. Implement `OrderService.cancelOrder()` — restores stock, changes status. Use `REQUIRES_NEW` if needed.
5. Implement payment simulation: `POST /api/v1/orders/{id}/pay` with body `{paymentMethod: "MOCK"}` — randomly succeeds 90% of the time (simulate real-world failure). Update `payment_status`.
6. Implement `OrderEventListener` — listens to internal `OrderCreatedEvent`, logs it (will publish to Kafka in Phase 5).
7. Flyway `V3__add_constraints.sql` — add named constraints, not inline. Show the difference.
8. Demonstrate N+1 fix: show the problem query in a comment, then fix with `@EntityGraph` or `JOIN FETCH`.

**Frontend Tasks:**
- Cart page: list items, quantities, update/remove, total price
- Checkout page: address form, order summary, place order button
- Payment page: "Pay Now" button, success/failure state
- Order history page: list of orders with status badge
- Order detail page: items, status timeline, cancel button

**Testing:**
- Unit test: `OrderServiceTest` — test stock deduction, rollback on insufficient stock
- Integration test: `OrderFlowIntegrationTest` — full flow: add to cart → place order → verify stock reduced

**After completion:** Generate `docs/PHASE_2_SUMMARY.md`

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 3 — MongoDB: Reviews and Activity Logs

**Goal:** Add MongoDB for document data. Understand when and why to use NoSQL alongside SQL.

**Concepts Introduced:**
- Polyglot persistence — using multiple databases for different data types
- Document model design — embedding vs referencing
- MongoDB aggregation pipeline
- Denormalization (storing `userName` in the review document)
- TTL indexes in MongoDB
- Cross-database data consistency (no FK across Postgres ↔ Mongo)

**Backend Tasks:**
1. Add `spring-boot-starter-data-mongodb` to `pom.xml`.
2. Configure MongoDB in `application.yml`. Add Mongo service to `docker-compose.yml`.
3. Implement `ProductReview` document. Explain: why store `userName` here even though it's in Postgres (avoid cross-DB joins).
4. Implement `UserActivityLog` document with TTL index configuration (90-day expiry). Explain TTL indexes.
5. Implement `Notification` document.
6. Implement `ReviewService`: create review (check user bought the product — cross-DB query), get reviews paginated, mark helpful.
7. After a review is created, trigger an async update to PostgreSQL: recalculate `avg_rating` and `review_count` on the product. Use `@Async`. Show this is a soft sync — eventual consistency.
8. Update `ActivityLogService`: now actually writes to MongoDB instead of console. The `@Async` from Phase 1 means this still doesn't slow down requests.
9. Aggregation pipeline: `ReviewRepository.getAverageRatingByProductId()` — demonstrate `$group`, `$avg`.
10. Add `NotificationService` — saves notifications to MongoDB (will be populated by Kafka in Phase 5, for now trigger directly on order placed).

**Frontend Tasks:**
- Review section on product detail page: star rating component, write review form, list of reviews
- "Was this helpful?" button on each review
- Verified purchase badge
- Notification bell icon in navbar: shows unread count, dropdown list

**Testing:**
- Integration test: `ReviewServiceIntegrationTest` with Testcontainers MongoDB

**After completion:** Generate `docs/PHASE_3_SUMMARY.md`

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 4 — Redis: Caching, Rate Limiting, Distributed Locks

**Goal:** Make the app fast. Learn cache strategies, Redis data structures, and distributed coordination.

**Concepts Introduced:**
- Cache-aside (lazy loading) vs write-through vs write-behind
- Cache invalidation — the hardest problem in CS, demonstrated practically
- Redis data structures: String, Hash, Sorted Set, and when to use each
- TTL and Redis eviction policies
- Rate limiting with sliding window using Redis
- Distributed locks — why `synchronized` fails across multiple servers
- Startup cache warming — parallel batch loading

**Backend Tasks:**
1. Add `spring-boot-starter-data-redis` and `spring-boot-starter-cache`.
2. Configure `CacheConfig.java` — create `RedisCacheManager` with per-cache TTL config. Explain why `CacheManager` exists.
3. Apply `@Cacheable("products")` on `ProductService.findById()`. Apply `@CacheEvict` on update and delete. Show what happens in Redis using the admin endpoint.
4. Cache product list queries: hash the query parameters to create a unique Redis key. Explain key design in comments.
5. Cache category list with 1-hour TTL.
6. Add `X-Cache: HIT | MISS` header to product endpoints so the frontend can display it.
7. Rate limiting: implement `RateLimitingFilter` using Redis `INCR` + `EXPIRE`. Limit: 100 requests/minute per IP for all APIs, 5 login attempts/minute for auth endpoints. Return `429 Too Many Requests` with `Retry-After` header when exceeded.
8. Distributed lock: implement `RedisDistributedLockService` using `SETNX` (SET if Not eXists). Apply it to flash sale stock deduction to show why it's needed. Add a comment showing why `synchronized` would fail if two instances of the app ran simultaneously.
9. Application startup: implement `CacheWarmingService` with `@EventListener(ApplicationReadyEvent.class)` — warm top 50 products using parallel `CompletableFuture` batch loading. Use `CountDownLatch` to wait. Log completion time.
10. Store JWT refresh tokens in Redis (was in-memory before). Update `AuthService`.

**Frontend Tasks:**
- Display `X-Cache: HIT | MISS` badge on product detail page (dev mode)
- Rate limit error page (pretty 429 page with countdown timer)
- Flash sale simulation: admin can mark a product as "flash sale" — triggers distributed lock demo
- Admin cache management page: show cache stats, ability to clear specific cache keys

**Testing:**
- Integration test: demonstrate cache hit vs miss with a counter on how many times the DB was queried
- Integration test: demonstrate rate limiting kicks in after N requests

**After completion:** Generate `docs/PHASE_4_SUMMARY.md`

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 5 — Kafka (KRaft Mode): Event-Driven Architecture

**Goal:** Decouple the application using events. Understand async messaging deeply.

**Concepts Introduced:**
- Why events decouple systems (producer doesn't care who consumes)
- Kafka internals: topics, partitions, offsets, consumer groups
- KRaft mode — what it replaces (Zookeeper) and why Kafka dropped it
- Message keys and partition assignment — why same user's events go to the same partition
- At-least-once delivery — why idempotency matters
- Dead letter topics — what happens when a consumer fails
- Manual acknowledgment — control exactly when a message is marked consumed

**Backend Tasks:**
1. Add `spring-kafka` to `pom.xml`.
2. Add Kafka to `docker-compose.yml` in **KRaft mode** (no Zookeeper):
   - Use `confluentinc/cp-kafka` image
   - Set environment: `KAFKA_NODE_ID=1`, `KAFKA_PROCESS_ROLES=broker,controller`, `KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093`, `CLUSTER_ID` (generate with `kafka-storage random-uuid`)
   - Explain each environment variable in a comment in `docker-compose.yml`
3. Add `provectuslabs/kafka-ui` Docker service for visual topic inspection.
4. Create `KafkaConfig.java` — define topics with correct partition count, configure producer and consumer factories. Explain serialization.
5. Implement `OrderEventProducer` — publishes to `order.placed` after order is created. Key = `userId`. Explain why this key choice ensures ordering.
6. Implement `InventoryEventProducer` — publishes to `inventory.updated` when stock changes.
7. Implement `NotificationConsumer` (group: `notification-service`) — listens to `notification.dispatch`, saves to MongoDB. Uses `AckMode.MANUAL`.
8. Implement `AnalyticsConsumer` (group: `analytics-service`) — listens to `user.activity`, saves to MongoDB.
9. Implement `InventorySyncConsumer` — listens to `inventory.updated`, will update Elasticsearch in Phase 6. For now, log it.
10. Configure Dead Letter Topic: if `NotificationConsumer` throws after 3 retries, send to `notification.dispatch.DLT`. Implement a `DLTHandler` that logs the failed message.
11. Update `OrderService.placeOrder()`: instead of directly calling `NotificationService`, publish to Kafka. The consumer handles saving the notification. Show the before/after difference.
12. Idempotency: use `orderId` as a check — if the same order event is consumed twice, don't create duplicate notifications.

**Frontend Tasks:**
- Notifications dropdown: poll `GET /api/v1/notifications` every 15 seconds
- Show toast notification when a new notification arrives
- Order status updates reflected in real-time (via polling)
- Admin Kafka dashboard: show topic names and consumer group lag (call the backend which queries Kafka AdminClient)

**Testing:**
- Integration test: `EmbeddedKafkaTest` — use `@EmbeddedKafka` to test producer → consumer flow without Docker

**After completion:** Generate `docs/PHASE_5_SUMMARY.md`

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 6 — Elasticsearch: Search and Discovery

**Goal:** Replace slow LIKE queries with real search. Understand how search engines work.

**Concepts Introduced:**
- How inverted indexes work (why search is O(1) not O(N))
- Text analysis — tokenization, stemming, stop words
- Full-text search vs keyword matching
- Fuzzy search — Levenshtein distance, why "iPhne" finds "iPhone"
- Faceted search — counts per category alongside results
- Search result highlighting
- Sync strategy: Kafka-based eventual consistency (product update → Kafka → ES index update)
- Why you don't just use PostgreSQL `ILIKE` (show it's slow on large datasets, no fuzzy, no ranking)

**Backend Tasks:**
1. Add `spring-data-elasticsearch` to `pom.xml`. Add Elasticsearch to `docker-compose.yml`.
2. Create `ProductDocument.java` with `@Document(indexName = "products")`. Define mapping explicitly — don't rely on dynamic mapping. Explain why.
3. Create `ProductSearchRepository` extending `ElasticsearchRepository`.
4. On application startup: if index doesn't exist, create it with the mapping defined in the architecture doc.
5. Implement `SearchService.search()`:
   - Full-text search across `name` (boosted 2x) and `description`
   - Filter by `category`, `price` range, `inStock`
   - Sort by relevance (default), price asc/desc, rating
   - Faceted aggregation: return `Map<category, count>` alongside results
   - Highlight matched terms in `name` and `description`
   - Fuzzy matching with `fuzziness: AUTO`
   - Pagination
6. Implement `SearchService.suggest()` — prefix search on `name.keyword` for autocomplete.
7. Sync strategy: update `InventorySyncConsumer` from Phase 5 — now when `inventory.updated` event arrives, update the Elasticsearch document. This demonstrates Kafka-as-sync-bus.
8. On product create/update (admin), also publish `inventory.updated` so ES stays in sync. This is dual-write pattern.
9. Admin reindex endpoint: `POST /api/v1/admin/search/reindex` — fetch all products from PostgreSQL, index into ES in parallel batches using `CountDownLatch` (from the threading plan). Return progress.
10. Add `GET /api/v1/admin/search/status` — returns index stats (doc count, index size).

**Frontend Tasks:**
- Search bar in navbar with autocomplete dropdown (debounced 300ms)
- Search results page:
  - Left sidebar: category filters with counts (facets), price range slider, in-stock toggle
  - Results: highlighted terms, price, rating, add to cart
  - Sort dropdown: relevance, price low→high, price high→low, rating
  - Pagination
- "Showing results for 'X'" + "Did you mean 'Y'?" typo suggestion

**Testing:**
- Integration test: `SearchServiceIntegrationTest` with Testcontainers Elasticsearch — test fuzzy search, facets, filtering

**After completion:** Generate `docs/PHASE_6_SUMMARY.md`

**DO NOT PROCEED UNTIL EXPLICITLY TOLD TO.**

---

### PHASE 7 — Production Readiness, Observability, and Azure Deployment

**Goal:** Make it shippable. Learn what "production ready" actually means.

**Concepts Introduced:**
- 12-factor app principles (with examples from this codebase)
- The three pillars of observability: metrics, logs, traces
- Structured logging — why JSON logs beat plain text
- Docker multi-stage builds — why they reduce image size
- CI/CD pipeline — every push tests and optionally deploys
- Java 21 Virtual Threads — Project Loom, the future of Java concurrency
- Health checks — liveness vs readiness probes

**Backend Tasks:**
1. **Virtual Threads (Java 21):** Add `spring.threads.virtual.enabled: true` to `application-prod.yml`. Write a comment explaining: what virtual threads are, why they replace large thread pools for I/O-bound work, and what "Project Loom" means.
2. **Structured JSON Logging:** Configure Logback with `logstash-logback-encoder`. Every log line is JSON with: `timestamp`, `level`, `logger`, `message`, `requestId` (from MDC), `userId`. Show how this enables log querying.
3. **Metrics with Micrometer:** Add `micrometer-registry-prometheus`. Expose `/actuator/prometheus`. Add custom metrics: `orders.placed.count`, `cache.hit.ratio`, `search.query.duration`.
4. **Prometheus + Grafana:** Add to `docker-compose.yml`. Create a Grafana dashboard JSON file with panels for: requests/second, p95 latency, cache hit ratio, order rate, active DB connections.
5. **Health Checks:** Implement custom `HealthIndicator` for: Kafka connectivity, Elasticsearch connectivity, Redis connectivity. These surface at `/actuator/health`.
6. **Docker Multi-Stage Build:**
   ```dockerfile
   # Stage 1: Build
   FROM maven:3.9-eclipse-temurin-21 AS builder
   # Stage 2: Runtime (much smaller)
   FROM eclipse-temurin:21-jre-jammy
   ```
   Comment explaining why two stages: builder image is 600MB, runtime image is 200MB.
7. **Azure Deployment:**
   - Create Azure Container Registry (ACR) — push backend and frontend images
   - Deploy with Azure Container Instances (ACI) or Azure App Service
   - Use Azure Database for PostgreSQL Flexible Server (managed)
   - Use Azure Cache for Redis (managed)
   - Create `docker-compose.azure.yml` that references Azure managed services instead of Docker containers
   - Store all secrets in Azure Key Vault, reference via environment variables
8. **GitHub Actions CI/CD** (`/.github/workflows/ci.yml`):
   ```yaml
   on: [push to main]
   jobs:
     test:    # Run unit + integration tests
     build:   # Build Docker images
     push:    # Push to ACR
     deploy:  # Deploy to Azure (manual approval gate)
   ```
9. **Security hardening checklist** (implement each, comment why):
   - CORS locked to frontend domain only
   - Rate limiting already done (Phase 4)
   - All secrets in environment variables — never in code or git
   - HTTPS only in production (configure in Azure)
   - Actuator endpoints secured — only `health` and `prometheus` exposed externally
   - Input sanitization — explain why JPA + parameterized queries prevent SQL injection automatically

**Frontend Tasks:**
- Admin observability dashboard: embed Grafana panels via iframe or build custom charts from `/actuator/prometheus` data
- System health status page: show green/red status for each service (Postgres, Redis, Kafka, ES)
- App performance improved: add loading skeletons, lazy-loaded routes

**Testing:**
- Load test simulation: write a simple test that hits 100 concurrent product view requests and verify cache hit ratio improves

**After completion:** Generate `docs/PHASE_7_SUMMARY.md`

---

## PHASE SUMMARY TEMPLATE

Every `docs/PHASE_X_SUMMARY.md` must contain:

```markdown
# Phase X Summary — [Technology Name]

## What Was Built
[List of features and files created]

## Technology Deep Dive
[Explain the technology introduced, from first principles, in simple language]

## Why This Technology?
[What problem does it solve? What would break without it?]

## Key Design Decisions
[For each significant decision, explain: what was chosen, what the alternative was, why this was better]

## How It Works Internally
[Explain the internals: e.g., how JPA translates objects to SQL, how Kafka stores messages]

## API Endpoints Added This Phase
[For each endpoint: method, path, auth required, sample request, sample response]

## Threading Concepts Used This Phase
[Explain what threading mechanism was used and why]

## MAANG Interview Questions
**Q1:** [Question]
**A1:** [Answer — thorough, interview-level]

**Q2:** [Question]
**A2:** [Answer]

**Q3:** [Question]
**A3:** [Answer]

## Trade-offs of This Approach
[What are the downsides of the choices made? What would you do differently at 10x scale?]

## How to Test This Phase
[Step-by-step instructions to verify everything works]
```

---

## Docker Compose Requirements

Every `docker-compose.yml` must have:
- `depends_on` with `condition: service_healthy` for every service dependency
- Health check defined for every service
- Named volumes for all persistent data
- A single shared `network: myshop-network`
- All credentials read from `.env` file, never hardcoded

---

## Code Quality Standards

- **No magic strings.** All Redis keys in `CacheKeys.java`. All Kafka topics in `KafkaTopics.java`.
- **No business logic in controllers.** Controllers only: validate input, call service, return response.
- **No entities in responses.** Always map to DTOs before returning.
- **Every public service method is `@Transactional` or explicitly marked `@Transactional(readOnly = true)`.**
- **All `@Async` methods return `void` or `CompletableFuture<T>`.** Never return a regular value from an async method.
- **Thread pool names are explicit.** `@Async("analyticsTaskExecutor")` not just `@Async`.
- **Flyway migrations are immutable.** Never edit a migration that has already run. Add new migrations for changes.
- **MapStruct for all mapping.** No manual `entity.getX(); dto.setX(entity.getX())` loops.

---

## Environment Variables (.env.example)

```env
# PostgreSQL
POSTGRES_DB=myshop
POSTGRES_USER=myshop_user
POSTGRES_PASSWORD=change_me_in_production
POSTGRES_PORT=5432

# MongoDB
MONGO_INITDB_DATABASE=myshop
MONGO_PORT=27017

# Redis
REDIS_PORT=6379
REDIS_PASSWORD=change_me_in_production

# Kafka
KAFKA_PORT=9092
KAFKA_CONTROLLER_PORT=9093
KAFKA_CLUSTER_ID=change_me_run_kafka_storage_uuid

# Elasticsearch
ES_PORT=9200

# Backend
BACKEND_PORT=8080
JWT_SECRET=change_me_minimum_32_characters_long
JWT_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=604800000
SPRING_PROFILES_ACTIVE=dev

# Frontend
FRONTEND_PORT=3000

# Azure (Phase 7)
AZURE_REGISTRY_NAME=
AZURE_RESOURCE_GROUP=
AZURE_LOCATION=eastus
```

---

## Git Configuration (Set Up in Phase 0)

### .gitignore
Create this `.gitignore` at the project root. The `docs/` folder containing all phase reports must be listed here so it is never committed or pushed to any remote repository.

```gitignore
# ─── Phase Reports (private — never push to git) ───────────────────────────
docs/

# ─── Environment & Secrets ──────────────────────────────────────────────────
.env
*.env
!.env.example

# ─── Java / Maven ───────────────────────────────────────────────────────────
target/
*.class
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar
hs_err_pid*
.mvn/wrapper/maven-wrapper.jar

# ─── Spring Boot ────────────────────────────────────────────────────────────
backend/logs/
backend/tmp/

# ─── Node / Frontend ────────────────────────────────────────────────────────
node_modules/
frontend/dist/
frontend/.vite/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# ─── Docker ─────────────────────────────────────────────────────────────────
docker/data/

# ─── IDE ─────────────────────────────────────────────────────────────────────
.idea/
*.iml
.vscode/
*.swp
*.swo
.DS_Store
Thumbs.db
```

### Why docs/ is gitignored
The `docs/PHASE_X_SUMMARY.md` files generated after each phase are your **personal learning notes**. They may contain internal implementation details, credentials used during development, or notes you don't want public. They live only on your local machine. If you want to back them up, use a **private** secondary repository or a local folder outside the project.

### Verify gitignore is working
After Phase 0, run:
```bash
git init
git status
# docs/ and .env should NOT appear in the output
```

---

## Getting Started (Phase 0 Instructions)

Prerequisites to verify before starting:
```bash
java -version        # Must be Java 21+
mvn -version         # Must be Maven 3.8+
node -version        # Must be Node 20+
docker -version      # Must be Docker 24+
docker compose version  # Must be v2+
```

After Phase 0 completes, verify with:
```bash
docker compose up --build
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
open http://localhost:3000
# Expected: myShop landing page
open http://localhost:8080/swagger-ui.html
# Expected: OpenAPI documentation
```
