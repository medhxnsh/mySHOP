package com.myshop.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.kafka.event.InventoryEvent;
import com.myshop.model.entity.OutboxEvent;
import com.myshop.repository.jpa.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxRelay relay;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxEventRepository, kafkaTemplate, objectMapper,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),
                io.micrometer.tracing.Tracer.NOOP);
    }

    private OutboxEvent pendingInventoryEvent() throws Exception {
        InventoryEvent payload = InventoryEvent.builder()
                .eventId("evt-1")
                .productId(UUID.randomUUID())
                .name("Headphones")
                .oldQuantity(10)
                .newQuantity(8)
                .reason("ORDER_PLACED")
                .build();
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .topic("inventory.updated")
                .partitionKey("p-1")
                .eventType("INVENTORY_UPDATED")
                .payload(objectMapper.writeValueAsString(payload))
                .payloadType(InventoryEvent.class.getName())
                .aggregateType("INVENTORY")
                .aggregateId("p-1")
                .build();
    }

    private CompletableFuture<SendResult<String, Object>> successfulSend() {
        return CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("t", "k", new Object()), null));
    }

    @Test
    void relay_publishesRehydratedPayloadAndMarksPublished() throws Exception {
        OutboxEvent event = pendingInventoryEvent();
        when(outboxEventRepository.findBatchForPublish()).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("inventory.updated"), eq("p-1"), any(InventoryEvent.class)))
                .thenReturn(successfulSend());

        relay.relayPendingEvents();

        // Rehydrated as the original class — consumers depend on the JSON
        // __TypeId__ header matching InventoryEvent.
        verify(kafkaTemplate).send(eq("inventory.updated"), eq("p-1"), any(InventoryEvent.class));
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void relay_sendFailure_incrementsAttemptsAndLeavesUnpublished() throws Exception {
        OutboxEvent event = pendingInventoryEvent();
        when(outboxEventRepository.findBatchForPublish()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay.relayPendingEvents();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
    }

    @Test
    void relay_stopsBatchAtFirstFailure_toPreserveOrdering() throws Exception {
        OutboxEvent first = pendingInventoryEvent();
        OutboxEvent second = pendingInventoryEvent();
        when(outboxEventRepository.findBatchForPublish()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay.relayPendingEvents();

        // Only ONE send attempted: skipping past a failed row could deliver
        // same-key events out of order.
        verify(kafkaTemplate).send(anyString(), anyString(), any());
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(second.getAttempts()).isZero();
    }

    @Test
    void relay_refusesForeignPayloadType() throws Exception {
        OutboxEvent event = pendingInventoryEvent();
        event.setPayloadType("java.lang.Runtime"); // must never be instantiated
        when(outboxEventRepository.findBatchForPublish()).thenReturn(List.of(event));

        relay.relayPendingEvents();

        verify(kafkaTemplate, org.mockito.Mockito.never()).send(anyString(), anyString(), any());
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
    }
}
