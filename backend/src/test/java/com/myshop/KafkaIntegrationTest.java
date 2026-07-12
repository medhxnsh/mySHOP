package com.myshop;

import com.myshop.constants.KafkaTopics;
import com.myshop.event.DomainEventPublisher;
import com.myshop.kafka.event.OrderEvent;
import com.myshop.model.enums.OrderStatus;
import com.myshop.repository.jpa.OutboxEventRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 6: end-to-end test of the outbox pipeline —
 * DomainEventPublisher (staged in a real DB transaction)
 * → OutboxRelay (@Scheduled) → Kafka → consumable typed record.
 *
 * Replaces the pre-outbox version of this test, which injected the deleted
 * OrderEventProducer and published directly.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "integration.tests", matches = "true")
// Random port — a fixed 9092 collides with the docker-compose Kafka on the host.
// @EmbeddedKafka injects its address into spring.kafka.bootstrap-servers, which
// KafkaConfig reads, so producer/consumer/relay all talk to the embedded broker.
@EmbeddedKafka(partitions = 1, topics = KafkaTopics.ORDER_PLACED)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KafkaIntegrationTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, Object> consumer;

    @BeforeAll
    void setUp() {
        Map<String, Object> configs = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(configs);
        consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.ORDER_PLACED);
    }

    @AfterAll
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void outboxPipeline_deliversStagedEventToKafka() throws Exception {
        String eventId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OrderEvent orderEvent = OrderEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .userId(userId)
                .email("test@example.com")
                .totalAmount(new BigDecimal("99.99"))
                .status(OrderStatus.PENDING)
                .build();

        // Stage the event exactly like OrderService does: inside a transaction.
        transactionTemplate.executeWithoutResult(status -> domainEventPublisher.publish(
                KafkaTopics.ORDER_PLACED,
                userId.toString(),
                "ORDER_PLACED",
                orderEvent,
                "ORDER",
                orderId.toString()));

        // The scheduled relay (500ms tick) picks it up and publishes.
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
        assertThat(records.count()).isGreaterThan(0);

        boolean found = false;
        for (ConsumerRecord<String, Object> record : records) {
            if (KafkaTopics.ORDER_PLACED.equals(record.topic()) && userId.toString().equals(record.key())) {
                OrderEvent received = (OrderEvent) record.value();
                if (eventId.equals(received.getEventId())) {
                    found = true;
                    assertThat(received.getOrderId()).isEqualTo(orderId);
                    assertThat(received.getEmail()).isEqualTo("test@example.com");
                    break;
                }
            }
        }
        assertThat(found).as("Expected OrderEvent was not delivered through the outbox relay").isTrue();

        // The outbox row must be marked delivered (poll briefly: marking commits
        // in the relay's transaction, slightly after the broker ack).
        boolean marked = false;
        for (int i = 0; i < 20 && !marked; i++) {
            marked = outboxEventRepository.findAll().stream()
                    .anyMatch(e -> orderId.toString().equals(e.getAggregateId())
                            && e.getPublishedAt() != null);
            if (!marked) {
                Thread.sleep(250);
            }
        }
        assertThat(marked).as("outbox row should be marked published_at after delivery").isTrue();
    }
}
