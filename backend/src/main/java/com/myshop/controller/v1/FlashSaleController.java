package com.myshop.controller.v1;

import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.FlashReservationResponse;
import com.myshop.dto.response.FlashSaleResponse;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.model.entity.FlashSaleReservation;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.FlashSaleReservationRepository;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.service.FlashPurchaseService;
import com.myshop.service.FlashSaleService;
import com.myshop.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Flash Sales", description = "High-concurrency flash-sale purchases (Redis hot path)")
@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final FlashPurchaseService flashPurchaseService;
    private final FlashSaleReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Operation(summary = "Currently active flash sale (public)")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> active() {
        return flashSaleService.findActive()
                .map(sale -> ResponseEntity.ok(ApiResponse.success(
                        FlashSaleResponse.from(sale, flashSaleService.remainingStock(sale.getId())))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null, "No active flash sale")));
    }

    @Operation(summary = "Buy one unit (one per user)", description = "Hot path: atomic Redis Lua + async order creation. "
            + "Returns 202 with a reservationId to poll — the durable order materializes moments later.")
    @PostMapping("/{id}/purchase")
    public ResponseEntity<ApiResponse<FlashReservationResponse>> purchase(@PathVariable UUID id) {
        UUID userId = currentUser().getId();
        UUID reservationId = flashPurchaseService.purchase(id, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(FlashReservationResponse.pending(reservationId),
                        "Purchase accepted — confirming your order"));
    }

    @Operation(summary = "Poll a reservation until the worker confirms it")
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<FlashReservationResponse>> reservation(@PathVariable UUID reservationId) {
        UUID userId = currentUser().getId();
        return reservationRepository.findById(reservationId)
                .map(r -> {
                    if (!r.getUserId().equals(userId)) {
                        // Don't leak other users' reservations — indistinguishable from not-yet-processed.
                        return ResponseEntity.ok(ApiResponse.success(
                                FlashReservationResponse.pending(reservationId)));
                    }
                    return ResponseEntity.ok(ApiResponse.success(new FlashReservationResponse(
                            r.getId(), r.getStatus(), r.getOrderId())));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        FlashReservationResponse.pending(reservationId))));
    }

    private User currentUser() {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
