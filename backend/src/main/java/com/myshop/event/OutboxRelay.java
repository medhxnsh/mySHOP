package com.myshop.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.config.MetricsConfig;
import com.myshop.model.entity.OutboxEvent;
import com.myshop.repository.jpa.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OutboxRelay — delivers pending outbox rows to Kafka.
 *
 * DELIVERY SEMANTICS: at-least-once. If the process crashes after the Kafka
 * ack but before the marking transaction commits, the row is re-delivered on
 * the next tick. Consumers must therefore be idempotent (NotificationConsumer
 * already deduplicates by orderId + type).
 *
 * ORDERING: rows are processed oldest-first, and the batch STOPS at the first
 * transient failure instead of skipping past it — skipping could deliver two
 * events for the same partition key out of order. The cost is head-of-line
 * blocking, which is bounded by the attempts cap below.
 *
 * POISON ROWS: a row that keeps failing (e.g. its payload class was renamed)
 * would block the queue forever, so rows reaching MAX_ATTEMPTS are excluded
 * from the polling query and logged at ERROR for manual repair (reset the
 * attempts column after fixing the cause).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "myshop.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    /** Kept in sync with the "attempts < 10" predicate in the polling query. */
    public static final int MAX_ATTEMPTS = 10;

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    /**
     * Transactional so the SKIP LOCKED row locks from findBatchForPublish()
     * are held until published_at/attempts updates commit — another instance
     * polling concurrently can never pick up the same rows.
     */
    @Scheduled(fixedDelayString = "${myshop.outbox.relay-interval-ms:500}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> batch = outboxEventRepository.findBatchForPublish();
        if (batch.isEmpty()) {
            return;
        }

        for (OutboxEvent event : batch) {
            try {
                Object payload = rehydrate(event);
                // Block for the broker ack: published_at must only be set for
                // events Kafka has durably accepted.
                sendWithRestoredTrace(event, payload);
                event.setPublishedAt(Instant.now());
                meterRegistry.counter(MetricsConfig.OUTBOX_PUBLISHED).increment();
                log.debug("Outbox event {} published to {}", event.getId(), event.getTopic());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                recordFailure(event, e);
                break;
            } catch (Exception e) {
                recordFailure(event, e);
                break; // preserve per-key ordering — retry from this row next tick
            }
        }
        // Dirty entities (publishedAt / attempts) flush when this transaction commits.
    }

    private void recordFailure(OutboxEvent event, Exception cause) {
        event.setAttempts(event.getAttempts() + 1);
        meterRegistry.counter(MetricsConfig.OUTBOX_FAILURES).increment();
        if (event.getAttempts() >= MAX_ATTEMPTS) {
            meterRegistry.counter(MetricsConfig.OUTBOX_STUCK).increment();
            log.error("Outbox event {} (type={}, topic={}) failed {} times and is now parked "
                    + "for manual repair — reset its attempts column after fixing the cause.",
                    event.getId(), event.getEventType(), event.getTopic(), event.getAttempts(), cause);
        } else {
            log.warn("Outbox event {} (type={}, topic={}) publish failed (attempt {}/{}): {}",
                    event.getId(), event.getEventType(), event.getTopic(),
                    event.getAttempts(), MAX_ATTEMPTS, cause.getMessage());
        }
    }

    /**
     * Rebuild the original event object so Spring's JsonSerializer writes the
     * same __TypeId__ header the @KafkaListener consumers already expect —
     * publishing the raw JSON string instead would break their deserialization.
     */
    private Object rehydrate(OutboxEvent event) throws Exception {
        String type = event.getPayloadType();
        // Only our own event classes may be instantiated from stored data.
        if (!type.startsWith("com.myshop.")) {
            throw new IllegalStateException("Refusing to rehydrate non-application payload type: " + type);
        }
        Class<?> clazz = Class.forName(type);
        return objectMapper.readValue(event.getPayload(), clazz);
    }

    /**
     * Publish inside the ORIGINAL request's trace context (persisted with the
     * event as a W3C traceparent). Without this, spans produced on this
     * scheduler thread start a fresh trace and the Kafka consumer's work never
     * connects back to the HTTP request that caused it. With it, one traceId
     * covers HTTP handler → outbox staging → relay send → consumer.
     */
    private void sendWithRestoredTrace(OutboxEvent event, Object payload) throws Exception {
        TraceContext parent = parseTraceParent(event.getTraceParent());
        if (parent == null) {
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return;
        }
        try (CurrentTraceContext.Scope ignored = tracer.currentTraceContext().newScope(parent)) {
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /** Parse "00-{32-hex traceId}-{16-hex spanId}-{flags}"; null if absent/malformed. */
    private TraceContext parseTraceParent(String traceParent) {
        if (traceParent == null || traceParent.isBlank()) {
            return null;
        }
        String[] parts = traceParent.split("-");
        if (parts.length != 4 || parts[1].length() != 32 || parts[2].length() != 16) {
            log.warn("Ignoring malformed traceparent on outbox event: {}", traceParent);
            return null;
        }
        return tracer.traceContextBuilder()
                .traceId(parts[1])
                .spanId(parts[2])
                .sampled(true)
                .build();
    }
}
