package com.myshop.controller.v1;

import com.myshop.dto.request.OrderStatusUpdateRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.OrderResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.service.OrderService;
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
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Order management APIs for Administrators")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')") // Strict Role check
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "View all orders across the system (can filter by status)")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<OrderResponse> orders = orderService.getAllOrders(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders, "All orders retrieved"));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update an order's fulfillment status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated via admin"));
    }
}
