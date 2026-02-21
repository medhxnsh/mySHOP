package com.myshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RegisterRequest — DTO for user registration.
 *
 * WHY SEPARATE REQUEST DTOs FROM ENTITIES?
 * 1. Validation: We validate user input here, not on the entity.
 * The entity assumes data is already clean.
 * 2. Security: We never expose entity fields like password_hash.
 * 3. Decoupling: API contract can evolve independently of DB schema.
 *
 * @Valid on the controller parameter triggers Bean Validation.
 *        If any constraint fails, Spring throws MethodArgumentNotValidException
 *        → caught by GlobalExceptionHandler → 400 Bad Request with field
 *        errors.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * Password rules:
     * - Minimum 8 characters
     * - At least 1 uppercase, 1 lowercase, 1 digit, 1 special character
     * WHY REGEX? Simple length checks allow "aaaaaaaa" — a terrible password.
     * This regex enforces real complexity without banning any characters.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$", message = "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
}
