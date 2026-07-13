package com.myshop.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.model.entity.OutboxEvent;
import com.myshop.repository.jpa.OutboxEventRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    @Override
    public void publish(String topic, String partitionKey, String eventType,
            Object payload, String aggregateType, String aggregateId) {

        // An outbox row committed WITHOUT the business change it describes is
        // exactly the ghost-event bug the outbox exists to prevent. Publishing
        // outside a transaction would auto-commit the row alone — fail loudly.
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "DomainEventPublisher.publish() must be called inside an active transaction "
                            + "(event: " + eventType + ", aggregate: " + aggregateType + "/" + aggregateId + ")");
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // Programming error (unserializable event class) — surface immediately
            // so the whole business transaction rolls back.
            throw new IllegalArgumentException(
                    "Failed to serialize outbox payload of type " + payload.getClass().getName(), e);
        }

        OutboxEvent event = OutboxEvent.builder()
                .topic(topic)
                .partitionKey(partitionKey)
                .eventType(eventType)
                .payload(json)
                .payloadType(payload.getClass().getName())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                // Persist the current trace context: the relay publishes later on
                // a scheduler thread, and without this the consumer's trace would
                // not connect back to the request that caused the event.
                .traceParent(currentTraceParent())
                .build();

        outboxEventRepository.save(event);
        log.debug("Outbox event staged: type={}, topic={}, aggregate={}/{}",
                eventType, topic, aggregateType, aggregateId);
    }

    /** W3C traceparent of the active span, or null when tracing is off. */
    private String currentTraceParent() {
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        return "00-" + span.context().traceId() + "-" + span.context().spanId() + "-01";
    }
}
