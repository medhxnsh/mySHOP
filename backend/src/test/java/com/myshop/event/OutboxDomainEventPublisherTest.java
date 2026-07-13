package com.myshop.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.kafka.event.OrderEvent;
import com.myshop.model.entity.OutboxEvent;
import com.myshop.model.enums.OrderStatus;
import com.myshop.repository.jpa.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private OutboxDomainEventPublisher publisher() {
        return new OutboxDomainEventPublisher(outboxEventRepository, new ObjectMapper(),
                io.micrometer.tracing.Tracer.NOOP);
    }

    @Test
    void publish_insideTransaction_savesOutboxRowWithSerializedPayload() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderEvent payload = OrderEvent.builder()
                .eventId("evt-1")
                .orderId(orderId)
                .userId(userId)
                .email("test@example.com")
                .totalAmount(new BigDecimal("42.50"))
                .status(OrderStatus.PENDING)
                .build();

        publisher().publish("order.placed", userId.toString(), "ORDER_PLACED",
                payload, "ORDER", orderId.toString());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getTopic()).isEqualTo("order.placed");
        assertThat(saved.getPartitionKey()).isEqualTo(userId.toString());
        assertThat(saved.getEventType()).isEqualTo("ORDER_PLACED");
        assertThat(saved.getAggregateType()).isEqualTo("ORDER");
        assertThat(saved.getAggregateId()).isEqualTo(orderId.toString());
        assertThat(saved.getPayloadType()).isEqualTo(OrderEvent.class.getName());
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getAttempts()).isZero();
        // Payload must round-trip: the relay deserializes this exact JSON.
        assertThat(saved.getPayload()).contains(orderId.toString()).contains("42.5");
    }

    @Test
    void publish_outsideTransaction_throwsAndSavesNothing() {
        assertThatThrownBy(() -> publisher().publish("order.placed", "k", "ORDER_PLACED",
                new Object(), "ORDER", "agg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active transaction");

        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
