package com.myshop.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product entity — Phase 0 skeleton.
 *
 * Key concepts illustrated here:
 * 1. @Version for optimistic locking
 * 2. BigDecimal for money (never double/float)
 * 3. ManyToOne relationship with Category
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(unique = true, nullable = false, length = 100)
    private String sku;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private int reviewCount = 0;

    /**
     * Many products belong to one category.
     * LAZY loading: we don't fetch the category unless accessed.
     * This prevents N+1 queries (explained in Phase 2).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * @Version — Optimistic locking.
     *          Hibernate manages this field automatically.
     *          On UPDATE, Hibernate adds "WHERE id=? AND version=?" to the SQL.
     *          If the row was modified since we read it (version changed), 0 rows
     *          update
     *          → Hibernate throws ObjectOptimisticLockingFailureException.
     *          This prevents two concurrent requests from deducting the same stock.
     */
    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
