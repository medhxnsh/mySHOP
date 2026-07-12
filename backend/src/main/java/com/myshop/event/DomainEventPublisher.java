package com.myshop.event;

/**
 * DomainEventPublisher — the port services use to emit integration events.
 *
 * WHY AN INTERFACE INSTEAD OF CALLING KafkaTemplate DIRECTLY?
 * 1. Dependency Inversion: business services depend on this abstraction;
 *    the outbox/Kafka machinery is an infrastructure detail behind it.
 * 2. The production implementation (OutboxDomainEventPublisher) writes the
 *    event to the outbox table INSIDE the caller's transaction — publishing
 *    and the business change commit or roll back together, which fixes the
 *    dual-write problem the direct producer calls had.
 * 3. Tests swap in an in-memory implementation without touching Kafka.
 */
public interface DomainEventPublisher {

    /**
     * Record a domain event for asynchronous delivery to Kafka.
     * Must be called inside an active transaction — the event only becomes
     * visible to the relay when that transaction commits.
     *
     * @param topic         destination topic (a KafkaTopics constant)
     * @param partitionKey  Kafka message key — chooses the partition, so it
     *                      defines ordering scope (userId, orderId, productId)
     * @param eventType     semantic name, e.g. "ORDER_PLACED"
     * @param payload       event object; serialized to JSON for storage and
     *                      rehydrated by the relay before sending
     * @param aggregateType aggregate the event belongs to, e.g. "ORDER"
     * @param aggregateId   identifier of that aggregate (for auditing)
     */
    void publish(String topic, String partitionKey, String eventType,
            Object payload, String aggregateType, String aggregateId);
}
