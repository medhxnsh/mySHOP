package com.myshop.controller.v1;

import com.myshop.dto.request.CreateFlashSaleRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.FlashSaleResponse;
import com.myshop.model.entity.FlashSale;
import com.myshop.service.FlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Admin lifecycle — /api/v1/admin/** is ADMIN-only via SecurityConfig. */
@Tag(name = "Admin: Flash Sales")
@RestController
@RequestMapping("/api/v1/admin/flash-sales")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminFlashSaleController {

    private final FlashSaleService flashSaleService;

    @Operation(summary = "Create a flash sale (DRAFT)")
    @PostMapping
    public ResponseEntity<ApiResponse<FlashSaleResponse>> create(
            @Valid @RequestBody CreateFlashSaleRequest request) {
        FlashSale sale = flashSaleService.create(request.productId(), request.salePrice(),
                request.totalStock(), request.startsAt(), request.endsAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(FlashSaleResponse.from(sale, null), "Flash sale created"));
    }

    @Operation(summary = "Activate: reserve product stock + pre-warm Redis")
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> activate(@PathVariable UUID id) {
        FlashSale sale = flashSaleService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(
                FlashSaleResponse.from(sale, flashSaleService.remainingStock(id)), "Flash sale activated"));
    }

    @Operation(summary = "End: stop purchases, return unsold stock to the product")
    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> end(@PathVariable UUID id) {
        FlashSale sale = flashSaleService.end(id);
        return ResponseEntity.ok(ApiResponse.success(
                FlashSaleResponse.from(sale, null), "Flash sale ended"));
    }
}
