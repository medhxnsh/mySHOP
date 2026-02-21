package com.myshop.repository.jpa;

import com.myshop.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

        Optional<Product> findBySku(String sku);

        boolean existsBySku(String sku);

        /** Paginated active products, optionally filtered by category */
        Page<Product> findByActiveTrueAndCategoryIdOrActiveTrueAndCategoryIdIsNull(
                        UUID categoryId, Pageable pageable);

        /**
         * Custom JPQL query for advanced filtering.
         * Phase 1 will add more filter parameters (price range, etc.)
         * JPQL operates on entity objects, not table columns.
         * ?:categoryId means "use this param, or match all if null"
         */
        @Query("SELECT p FROM Product p WHERE p.active = true " +
                        "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
        Page<Product> findActiveProducts(
                        @Param("categoryId") UUID categoryId,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);
}
