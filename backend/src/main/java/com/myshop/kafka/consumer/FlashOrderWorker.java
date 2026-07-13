package com.myshop.kafka.consumer;

import com.myshop.constants.KafkaTopics;
import com.myshop.kafka.event.FlashOrderEvent;
import com.myshop.service.FlashOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * FlashOrderWorker — drains order.flash.requested into durable orders.
 * The hot path stays fast because THIS is where the database work happens.
 *
 * Failures are not acked → container retry ×3 → DLT (a poison reservation
 * never blocks the partition forever). A unique-constraint violation is the
 * idempotency guard firing under race — treated as success.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashOrderWorker {

    private final FlashOrderService flashOrderService;

    @KafkaListener(topics = KafkaTopics.ORDER_FLASH_REQUESTED, groupId = "flash-order-worker")
    public void onFlashOrderRequested(@Payload FlashOrderEvent event, Acknowledgment acknowledgment) {
        try {
            flashOrderService.materializeReservation(event);
        } catch (DataIntegrityViolationException e) {
            // Two redeliveries raced past the exists() check — the constraint
            // did its job; the reservation is already materialized.
            log.info("Flash reservation {} hit the idempotency constraint — already materialized",
                    event.getReservationId());
        }
        acknowledgment.acknowledge();
    }
}
