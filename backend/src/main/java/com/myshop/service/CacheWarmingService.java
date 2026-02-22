package com.myshop.service;

import com.myshop.model.entity.Product;
import com.myshop.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * Cache Warming Service.
 * Runs on ApplicationReadyEvent (when Spring context is fully initialized).
 * Fetches the newest or most popular 50 products and populates the Redis cache.
 * Uses CompletableFuture for parallel execution to avoid blocking startup,
 * and CountDownLatch to coordinate logging when all are finished.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmingService implements ApplicationListener<ApplicationReadyEvent> {

    private final ProductRepository productRepository;

    // We inject ProductService to call its @Cacheable method, which triggers
    // caching.
    // If we just called the repository, it wouldn't hit the Cache interceptor.
    private final ProductService productService;

    @Override
    @Async
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Starting Cache Warming for top 50 products...");
        long startTime = System.currentTimeMillis();

        // Get 50 newest products as our "popular" heuristic for this demo
        Page<Product> topProductsPage = productRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Product> topProducts = topProductsPage.getContent();
        if (topProducts.isEmpty()) {
            log.info("No products found to warm cache.");
            return;
        }

        CountDownLatch latch = new CountDownLatch(topProducts.size());

        for (Product product : topProducts) {
            CompletableFuture.runAsync(() -> {
                try {
                    // Call the Cacheable method
                    productService.getById(product.getId());
                    log.debug("Warmed cache for product: {}", product.getId());
                } catch (Exception e) {
                    log.error("Failed to warm cache for product: {}", product.getId(), e);
                } finally {
                    latch.countDown();
                }
            }); // Utilizes ForkJoinPool.commonPool() implicitly
        }

        try {
            latch.await();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warming completed successfully for {} products in {} ms", topProducts.size(), duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cache warming interrupted", e);
        }
    }
}
