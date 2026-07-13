package com.myshop.dto.response;

import com.myshop.model.entity.FlashSale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FlashSaleResponse(
        UUID id,
        UUID productId,
        String productName,
        String productImageUrl,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int totalStock,
        Integer remainingStock, // null when the sale isn't warmed in Redis
        Instant startsAt,
        Instant endsAt,
        String status) {

    public static FlashSaleResponse from(FlashSale sale, Integer remainingStock) {
        return new FlashSaleResponse(
                sale.getId(),
                sale.getProduct().getId(),
                sale.getProduct().getName(),
                sale.getProduct().getImageUrl(),
                sale.getProduct().getPrice(),
                sale.getSalePrice(),
                sale.getTotalStock(),
                remainingStock,
                sale.getStartsAt(),
                sale.getEndsAt(),
                sale.getStatus().name());
    }
}
