package com.myshop.service;

import com.myshop.dto.request.CreateProductRequest;
import com.myshop.dto.request.UpdateProductRequest;
import com.myshop.dto.response.PagedResponse;
import com.myshop.dto.response.ProductResponse;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.mapper.ProductMapper;
import com.myshop.model.entity.Category;
import com.myshop.model.entity.Product;
import com.myshop.repository.jpa.CategoryRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ProductService — the core business logic for the product catalog.
 *
 * KEY CONCEPTS ILLUSTRATED:
 *
 * 1. @Transactional(readOnly=true) on GET methods:
 * readOnly=true tells Hibernate to skip dirty-checking (tracking changes).
 * This is a performance win on read-heavy endpoints.
 * Hibernate won't snapshot the entity state at load time or compare at flush.
 * IMPORTANT: readOnly requests CAN'T write — Hibernate skips flush entirely.
 *
 * 2. @Transactional on write methods:
 * If productRepository.save() succeeds but activityLogService fails,
 * the transaction ROLLS BACK — no partial state.
 * (ActivityLogService is @Async, so it runs AFTER the transaction commits —
 * it won't cause rollback even if async work fails. This is intentional.)
 *
 * 3. PATCH semantics via MapStruct @MappingTarget:
 * productMapper.updateEntity(product, request) modifies the existing entity
 * in-place.
 * Null fields in request are IGNORED (NullValuePropertyMappingStrategy.IGNORE).
 * Only non-null fields from UpdateProductRequest are applied.
 * Then we just save the modified entity — Hibernate detects changes via dirty
 * checking.
 *
 * 4. Soft Delete:
 * We never DELETE rows from the products table. Products that are removed
 * have isActive=false. This preserves order history (order_items reference
 * products).
 * Deleting the row → FK violation from order_items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;
        private final ProductMapper productMapper;
        private final ActivityLogService activityLogService;

        @Cacheable(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS_PAGED, key = "T(java.util.Objects).hash(#page, #size, #categoryId, #minPrice, #maxPrice, #sortBy, #sortDir)")
        @Transactional(readOnly = true)
        public PagedResponse<ProductResponse> getAll(int page, int size,
                        UUID categoryId,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        String sortBy,
                        String sortDir) {

                Sort sort = sortDir.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Product> productPage = productRepository.findActiveProducts(
                                categoryId, minPrice, maxPrice, pageable);

                List<ProductResponse> content = productPage.getContent()
                                .stream()
                                .map(productMapper::toResponse)
                                .toList();

                return PagedResponse.of(productPage, content);
        }

        @Cacheable(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS, key = "#id")
        @Transactional(readOnly = true)
        public ProductResponse getById(UUID id) {
                Product product = productRepository.findById(id)
                                .filter(Product::isActive)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id.toString()));

                // Async: log product view — won't block the HTTP response
                String currentUser = SecurityUtils.getCurrentUserEmail().orElse("anonymous");

                // TODO Phase 3: Verify async wiring. This runs in "analyticsTaskExecutor" and
                // logs to console.
                // In Phase 3, this will save a document to MongoDB.
                activityLogService.logActivity(currentUser, "PRODUCT_VIEWED", "PRODUCT", id.toString());

                return productMapper.toResponse(product);
        }

        @org.springframework.cache.annotation.CacheEvict(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS_PAGED, allEntries = true)
        @Transactional
        public ProductResponse create(CreateProductRequest request) {
                // Guard: SKU must be globally unique
                if (productRepository.existsBySku(request.getSku())) {
                        throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS,
                                        "SKU already in use: " + request.getSku());
                }

                Product product = productMapper.toEntity(request);

                // Set category if provided
                if (request.getCategoryId() != null) {
                        Category category = categoryRepository.findById(request.getCategoryId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Category", "id", request.getCategoryId().toString()));
                        product.setCategory(category);
                }

                product.setActive(true);
                Product saved = productRepository.save(product);
                log.info("Product created: {} (SKU: {})", saved.getName(), saved.getSku());

                return productMapper.toResponse(saved);
        }

        @org.springframework.cache.annotation.Caching(evict = {
                        @org.springframework.cache.annotation.CacheEvict(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS, key = "#id"),
                        @org.springframework.cache.annotation.CacheEvict(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS_PAGED, allEntries = true)
        })
        @Transactional
        public ProductResponse update(UUID id, UpdateProductRequest request) {
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id.toString()));

                // Check SKU uniqueness only if SKU is being changed
                if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
                        if (productRepository.existsBySku(request.getSku())) {
                                throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS,
                                                "SKU already in use: " + request.getSku());
                        }
                }

                // PATCH: apply only non-null fields from request onto the entity
                productMapper.updateEntity(product, request);

                // Update category if a new categoryId is provided
                if (request.getCategoryId() != null) {
                        Category category = categoryRepository.findById(request.getCategoryId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Category", "id", request.getCategoryId().toString()));
                        product.setCategory(category);
                }

                Product updated = productRepository.save(product);
                log.info("Product updated: {}", updated.getId());

                return productMapper.toResponse(updated);
        }

        @org.springframework.cache.annotation.Caching(evict = {
                        @org.springframework.cache.annotation.CacheEvict(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS, key = "#id"),
                        @org.springframework.cache.annotation.CacheEvict(value = com.myshop.config.CacheConfig.CACHE_PRODUCTS_PAGED, allEntries = true)
        })
        @Transactional
        public void delete(UUID id) {
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id.toString()));

                // Soft delete — set isActive=false, don't DELETE the row
                product.setActive(false);
                productRepository.save(product);
                log.info("Product soft-deleted: {}", id);
        }
}
