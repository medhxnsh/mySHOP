package com.myshop.repository.jpa;

import com.myshop.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * N+1 QUERY PROBLEM EXPLANATION:
     * When fetching a list of Orders (e.g. 10 orders), if we access the 'items'
     * collection
     * which is @OneToMany(fetch = LAZY), Hibernate will execute 1 query for the
     * orders,
     * and then 10 additional queries (N) to fetch the items for each order.
     * 
     * WHY IT MATTERS:
     * This destroys performance and crushes the database connection pool with
     * hundreds
     * of tiny overhead network calls.
     * 
     * SOLUTION:
     * 
     * @EntityGraph(attributePaths = {"items"}) tells Hibernate to use a SQL 'LEFT
     *                             OUTER JOIN'
     *                             to fetch both the order and its items in a SINGLE
     *                             query.
     */
    @EntityGraph(attributePaths = { "items" })
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = { "items" })
    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
