package com.myshop.service;

import com.myshop.event.internal.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * OrderEventListener listens to internal Spring application events.
 * 
 * Demonstrates CompletableFuture by kicking off background work
 * after the main order transaction has committed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    // Using the specific pools we created in Phase 1
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrder().getId());

        // 1. Simulate sending confirmation email (Async)
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Simulating email send for order {}, user {}",
                        event.getOrder().getId(), event.getOrder().getUser().getEmail());
                Thread.sleep(1500); // simulate network delay
                log.info("Async email sent for order: {}", event.getOrder().getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 2. Log analytics event (Async)
        CompletableFuture.runAsync(() -> {
            activityLogService.logActivity(
                    event.getOrder().getUser().getEmail(),
                    "ORDER_PLACED",
                    "ORDER",
                    event.getOrder().getId().toString());
            log.info("Analytics event logged for order: {}", event.getOrder().getId());
        });
        // 3. Send Notification via MongoDB (Async)
        CompletableFuture.runAsync(() -> {
            notificationService.createNotification(
                    event.getOrder().getUser().getId(),
                    "ORDER_CONFIRMED",
                    "Order Confirmed!",
                    "Your order with " + event.getOrder().getItems().size() + " item(s) has been successfully placed.",
                    java.util.Map.of("orderId", event.getOrder().getId().toString(), "status", "PENDING"));
        });
    }
}
