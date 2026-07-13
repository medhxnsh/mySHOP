package com.myshop.service;

import com.myshop.constants.AppConstants;
import com.myshop.constants.KafkaTopics;
import com.myshop.event.DomainEventPublisher;
import com.myshop.kafka.event.FlashOrderEvent;
import com.myshop.kafka.event.OrderEvent;
import com.myshop.model.entity.FlashSaleReservation;
import com.myshop.model.entity.Order;
import com.myshop.model.entity.OrderItem;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.model.enums.OrderStatus;
import com.myshop.model.enums.PaymentStatus;
import com.myshop.repository.jpa.FlashSaleReservationRepository;
import com.myshop.repository.jpa.OrderRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * FlashOrderService — turns an accepted flash reservation into a durable
 * order (called by FlashOrderWorker off the Kafka topic).
 *
 * One ACID transaction: order + order_item + reservation row + the
 * ORDER_PLACED outbox event (back on the Phase 6 rails — from here the flash
 * flow rejoins the normal order pipeline: notifications, analytics, tracing).
 *
 * IDEMPOTENT: Kafka delivers at-least-once; UNIQUE(sale_id, user_id) on the
 * reservation makes redelivery a no-op (checked up front, enforced by the
 * constraint under race).
 *
 * NOTE: product stock is NOT decremented here — activation already moved the
 * sale's units out of products.stock_quantity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashOrderService {

    private final FlashSaleReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public void materializeReservation(FlashOrderEvent event) {
        if (reservationRepository.existsBySaleIdAndUserId(event.getSaleId(), event.getUserId())) {
            log.info("Flash reservation {} already materialized — skipping (at-least-once redelivery)",
                    event.getReservationId());
            return;
        }

        User user = userRepository.findById(event.getUserId()).orElseThrow();
        Product product = productRepository.findById(event.getProductId()).orElseThrow();
        BigDecimal price = event.getSalePrice();

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.COD)
                .totalAmount(price)
                .shippingAddress(Map.of("note", "FLASH_SALE — address to be confirmed"))
                .build();
        order.getItems().add(OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .unitPrice(price)
                .subtotal(price)
                .build());
        Order savedOrder = orderRepository.save(order);

        reservationRepository.save(FlashSaleReservation.builder()
                .id(event.getReservationId())
                .saleId(event.getSaleId())
                .userId(event.getUserId())
                .status("CONFIRMED")
                .orderId(savedOrder.getId())
                .build());

        domainEventPublisher.publish(
                KafkaTopics.ORDER_PLACED,
                user.getId().toString(),
                AppConstants.EVENT_ORDER_PLACED,
                OrderEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(savedOrder.getId())
                        .userId(user.getId())
                        .email(user.getEmail())
                        .totalAmount(savedOrder.getTotalAmount())
                        .status(savedOrder.getStatus())
                        .build(),
                AppConstants.AGGREGATE_ORDER,
                savedOrder.getId().toString());

        log.info("Flash reservation {} materialized as order {} (sale {}, user {})",
                event.getReservationId(), savedOrder.getId(), event.getSaleId(), user.getEmail());
    }
}
