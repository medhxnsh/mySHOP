package com.myshop.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Category entity — supports hierarchical categories via parent_id
 * self-reference.
 */
@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    /**
     * Self-referencing: a category can have a parent category.
     * parent = null means this is a root/top-level category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** Subcategories of this category */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @ToString.Exclude // Prevent infinite recursion in toString()
    @EqualsAndHashCode.Exclude
    private List<Category> children;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
