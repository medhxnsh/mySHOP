package com.myshop.controller.v1;

import com.myshop.dto.request.CartItemRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.CartResponse;
import com.myshop.service.CartService;
import com.myshop.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('USER')") // All cart endpoints require user login
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        CartResponse cart = cartService.getCartByUser(email);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart retrieved successfully"));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        CartResponse cart = cartService.addItem(email, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart"));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update item quantity in cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        CartResponse cart = cartService.updateItemQuantity(email, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart item quantity updated"));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable UUID productId) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        CartResponse cart = cartService.removeItem(email, productId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart"));
    }

    @DeleteMapping
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        cartService.clearCart(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully"));
    }
}
