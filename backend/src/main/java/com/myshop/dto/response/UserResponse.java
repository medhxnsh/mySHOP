package com.myshop.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * UserResponse — safe representation of a User for API responses.
 *
 * WHY NOT RETURN THE USER ENTITY DIRECTLY?
 * The User entity has password_hash, isActive, and other internal fields
 * we never want to expose to clients. This DTO is the "safe view".
 * MapStruct maps User → UserResponse at compile time (zero reflection
 * overhead).
 */
@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private Instant createdAt;
}
