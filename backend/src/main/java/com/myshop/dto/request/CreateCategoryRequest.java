package com.myshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** CreateCategoryRequest — input for creating a new category. */
@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 255)
    private String name;

    /**
     * Slug must be URL-safe: lowercase letters, digits, hyphens only.
     * Examples: "home-kitchen", "sports-fitness", "books"
     * Used in URLs: /products?category=home-kitchen
     */
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase letters, digits, and hyphens only (e.g. home-kitchen)")
    private String slug;

    /** Optional parent — null means this is a root category */
    private UUID parentId;
}
