package com.myshop.kafka.consumer;

import com.myshop.kafka.event.OrderEvent;
import com.myshop.repository.mongo.NotificationRepository;
import com.myshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "order.placed", groupId = "notification-service")
    public void consumeOrderPlacedEvent(@Payload OrderEvent event, Acknowledgment acknowledgment) {
        log.info("NotificationConsumer received OrderEvent: {}", event);

        String orderIdStr = event.getOrderId().toString();
        String type = "ORDER_CONFIRMED";

        // Idempotency check: Have we already sent an ORDER_CONFIRMED notification for
        // this order?
        if (notificationRepository.existsByMetadataOrderIdAndType(orderIdStr, type)) {
            log.warn("Duplicate notification detected for orderId: {}", orderIdStr);
            acknowledgment.acknowledge();
            return;
        }

        notificationService.createNotification(
                event.getUserId(),
                type,
                "Order Confirmed",
                "Your order has been successfully placed. Total: $" + event.getTotalAmount(),
                Map.of("orderId", orderIdStr));

        // MANUAL ACKNOWLEDGE
        acknowledgment.acknowledge();
        log.info("Acknowledged processing of OrderEvent in NotificationConsumer.");
    }
}
