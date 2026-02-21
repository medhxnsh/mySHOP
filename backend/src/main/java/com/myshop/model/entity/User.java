package com.myshop.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity — Phase 0 skeleton.
 * Full implementation with security details completed in Phase 1.
 *
 * WHY @Entity?
 * Marks this class as a JPA managed entity. JPA maps this class to the
 * 'users' table in PostgreSQL. Hibernate uses it to generate SQL queries.
 *
 * WHY @Data from Lombok?
 * Generates: getters, setters, equals(), hashCode(), toString().
 * Without Lombok, you'd write hundreds of lines of boilerplate.
 * WARNING: @Data's equals/hashCode uses ALL fields by default.
 * For JPA entities, override to only use the ID (or
 * use @EqualsAndHashCode(of="id")).
 *
 * WHY @Builder?
 * Enables: User.builder().email("...").fullName("...").build()
 * Much more readable than: new User(); user.setEmail(...);
 * user.setFullName(...)
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor // Required by JPA — it uses default constructor to create instances
@AllArgsConstructor // Required by @Builder — it needs all-args constructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

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
