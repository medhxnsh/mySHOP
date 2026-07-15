package com.myshop.repository.jpa;

import com.myshop.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    boolean existsByOrderUserIdAndProductId(UUID userId, UUID productId);

    /** Products this user already bought — excluded from recommendations (Phase 10). */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT oi.product.id FROM OrderItem oi WHERE oi.order.user.id = :userId")
    List<UUID> findPurchasedProductIds(UUID userId);

    /** Best sellers by units sold — the popularity fallback (Phase 10). */
    @org.springframework.data.jpa.repository.Query("SELECT oi.product.id FROM OrderItem oi WHERE oi.product.active = true "
            + "GROUP BY oi.product.id ORDER BY SUM(oi.quantity) DESC")
    List<UUID> findBestSellingProductIds(org.springframework.data.domain.Pageable pageable);
}
