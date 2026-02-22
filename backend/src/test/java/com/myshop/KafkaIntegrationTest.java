package com.myshop;

import com.myshop.kafka.event.OrderEvent;
import com.myshop.kafka.producer.OrderEventProducer;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import com.myshop.model.enums.OrderStatus;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "integration.tests", matches = "true")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KafkaIntegrationTest {

    @Autowired
    private OrderEventProducer orderEventProducer;

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
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterAll
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testOrderPlacementPublishesEvent() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Create dummy event explicitly to avoid Mockito proxying serialization issues
        OrderEvent orderEvent = OrderEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .userId(userId)
                .email("test@example.com")
                .totalAmount(new BigDecimal("99.99"))
                .status(OrderStatus.PENDING) // Changed to use OrderStatus enum
                .build();

        // Act
        orderEventProducer.publishOrderPlaced(orderEvent);

        // Assert
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThan(0);

        boolean found = false;
        for (ConsumerRecord<String, Object> record : records) {
            if ("order.placed".equals(record.topic()) && userId.toString().equals(record.key())) {
                OrderEvent received = (OrderEvent) record.value();
                if (eventId.equals(received.getEventId())) {
                    found = true;
                    assertThat(received.getOrderId()).isEqualTo(orderId);
                    assertThat(received.getEmail()).isEqualTo("test@example.com");
                    break;
                }
            }
        }

        assertThat(found).as("Expected OrderEvent was not found in the Kafka topic").isTrue();
    }
}
