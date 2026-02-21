package com.myshop.controller.v1;

import com.myshop.dto.request.OrderRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.OrderResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.service.OrderService;
import com.myshop.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs for Users")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('USER')") // All these endpoints require login
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order from current cart")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest request) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        OrderResponse order = orderService.placeOrder(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order placed successfully"));
    }

    @GetMapping
    @Operation(summary = "Get current user's order history")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        PagedResponse<OrderResponse> orders = orderService.getUserOrders(email, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders, "Order history retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific order details")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        OrderResponse order = orderService.getOrderById(email, id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order details retrieved"));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID id) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        OrderResponse order = orderService.cancelOrder(email, id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Simulate payment for an order")
    public ResponseEntity<ApiResponse<OrderResponse>> simulatePayment(@PathVariable UUID id) {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        OrderResponse order = orderService.simulatePayment(email, id);
        return ResponseEntity.ok(ApiResponse.success(order, "Payment processed successfully"));
    }
}
