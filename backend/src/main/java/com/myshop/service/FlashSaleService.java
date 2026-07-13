package com.myshop.service;

import com.myshop.constants.CacheKeys;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.model.entity.FlashSale;
import com.myshop.model.entity.Product;
import com.myshop.model.enums.FlashSaleStatus;
import com.myshop.repository.jpa.FlashSaleRepository;
import com.myshop.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * FlashSaleService — admin lifecycle for flash sales (Phase 9).
 *
 * ACTIVATION is the interesting part: it moves inventory INTO the sale.
 * The sale's units are deducted from products.stock_quantity up front (one
 * ACID transaction, optimistic-locked), then Redis is pre-warmed with the
 * stock counter + sale metadata. From that moment the hot path runs entirely
 * on Redis; regular checkout can never sell the same units because the
 * product stock no longer includes them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public FlashSale create(UUID productId, BigDecimal salePrice, int totalStock,
            Instant startsAt, Instant endsAt) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId.toString()));

        return flashSaleRepository.save(FlashSale.builder()
                .product(product)
                .salePrice(salePrice)
                .totalStock(totalStock)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(FlashSaleStatus.DRAFT)
                .build());
    }

    @Transactional
    public FlashSale activate(UUID saleId) {
        FlashSale sale = flashSaleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", "id", saleId.toString()));

        if (sale.getStatus() != FlashSaleStatus.DRAFT) {
            throw new BusinessException(ErrorCode.FLASH_SALE_INVALID_STATE,
                    "Only DRAFT sales can be activated (current: " + sale.getStatus() + ")");
        }

        Product product = sale.getProduct();
        if (product.getStockQuantity() < sale.getTotalStock()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Product has " + product.getStockQuantity() + " units but the sale needs "
                            + sale.getTotalStock());
        }

        // Reserve the sale's inventory from the product (optimistic-locked).
        product.setStockQuantity(product.getStockQuantity() - sale.getTotalStock());
        productRepository.save(product);

        sale.setStatus(FlashSaleStatus.ACTIVE);
        flashSaleRepository.save(sale);

        // Pre-warm Redis: stock counter + metadata for the DB-free hot path.
        redisTemplate.opsForValue().set(
                CacheKeys.format(CacheKeys.FLASH_STOCK, saleId),
                String.valueOf(sale.getTotalStock()));
        redisTemplate.opsForHash().putAll(
                CacheKeys.format(CacheKeys.FLASH_META, saleId),
                Map.of("productId", product.getId().toString(),
                        "salePrice", sale.getSalePrice().toPlainString()));

        log.info("Flash sale {} ACTIVE: {} units of '{}' at {} (product stock now {})",
                saleId, sale.getTotalStock(), product.getName(), sale.getSalePrice(),
                product.getStockQuantity());
        return sale;
    }

    /**
     * End a sale: hot-path keys are removed (purchases now reject with
     * NOT_ACTIVE) and unsold units return to the product.
     */
    @Transactional
    public FlashSale end(UUID saleId) {
        FlashSale sale = flashSaleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", "id", saleId.toString()));
        if (sale.getStatus() != FlashSaleStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FLASH_SALE_INVALID_STATE,
                    "Only ACTIVE sales can be ended (current: " + sale.getStatus() + ")");
        }

        String stockKey = CacheKeys.format(CacheKeys.FLASH_STOCK, saleId);
        String remaining = redisTemplate.opsForValue().get(stockKey);
        int unsold = remaining == null ? 0 : Math.max(0, Integer.parseInt(remaining));

        // Remove the stock key FIRST — from here the Lua script rejects all buyers.
        redisTemplate.delete(stockKey);
        redisTemplate.delete(CacheKeys.format(CacheKeys.FLASH_META, saleId));
        // buyers set is kept for the reconciliation job / audit.

        if (unsold > 0) {
            Product product = sale.getProduct();
            product.setStockQuantity(product.getStockQuantity() + unsold);
            productRepository.save(product);
        }

        sale.setStatus(FlashSaleStatus.ENDED);
        log.info("Flash sale {} ENDED: {} unsold units returned to product stock", saleId, unsold);
        return flashSaleRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public Optional<FlashSale> findActive() {
        return flashSaleRepository.findFirstByStatusOrderByStartsAtAsc(FlashSaleStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public FlashSale getById(UUID saleId) {
        return flashSaleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", "id", saleId.toString()));
    }

    /** Remaining Redis stock for an active sale (null if not warmed). */
    public Integer remainingStock(UUID saleId) {
        String value = redisTemplate.opsForValue().get(CacheKeys.format(CacheKeys.FLASH_STOCK, saleId));
        return value == null ? null : Integer.parseInt(value);
    }
}
