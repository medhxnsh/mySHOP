package com.myshop.controller.v1;

import com.myshop.constants.AppConstants;
import com.myshop.dto.request.CreateProductRequest;
import com.myshop.dto.request.UpdateProductRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.dto.response.ProductResponse;
import com.myshop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ProductController — public read, ADMIN-only write.
 *
 * WHY ARE AUTHORIZATION RULES IN SecurityConfig AND NOT HERE?
 * Centralised security rules in SecurityConfig are easier to audit.
 * You can see ALL protected routes in one place.
 * 
 * @PreAuthorize on each method requires reading every controller to understand
 *               the security posture.
 *               For simple role-based access, SecurityConfig URL matchers are
 *               cleaner.
 *
 *               PAGINATION PARAMETERS:
 *               page (0-indexed), size, sortBy, sortDir
 *               Defaults from AppConstants to avoid magic numbers and keep them
 *               consistent across endpoints.
 */
@Tag(name = "Products", description = "Product catalog — public read, ADMIN write")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all active products (paginated, filterable)")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,

            @Parameter(description = "Items per page") @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE_STR) int size,

            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<ProductResponse> response = productService.getAll(
                page, size, categoryId, minPrice, maxPrice, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get a product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @Operation(summary = "Create a new product (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product));
    }

    @Operation(summary = "Update a product (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {

        return ResponseEntity.ok(ApiResponse.success(productService.update(id, request)));
    }

    @Operation(summary = "Soft-delete a product (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);
        // 204 No Content — delete succeeded, nothing to return
        return ResponseEntity.noContent().build();
    }
}
