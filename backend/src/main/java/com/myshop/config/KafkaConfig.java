package com.myshop.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration
 * Defines topics, producer and consumer factories, and error handling for
 * Kafka.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    // ========================================================================
    // 1. TOPIC DEFINITIONS (5 Topics)
    // ========================================================================

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order.placed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusUpdatedTopic() {
        return TopicBuilder.name("order.status.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryUpdatedTopic() {
        return TopicBuilder.name("inventory.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userActivityTopic() {
        return TopicBuilder.name("user.activity")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationDispatchTopic() {
        return TopicBuilder.name("notification.dispatch")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dltTopic() {
        return TopicBuilder.name("myshop.dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ========================================================================
    // 2. PRODUCER CONFIGURATION
    // ========================================================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Keys are Strings (e.g. userId or orderId)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Values are serialized to JSON. This allows sending complex Java objects over
        // the network.
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========================================================================
    // 3. CONSUMER CONFIGURATION
    // ========================================================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "myshop-group"); // Default group
        // Keys: String
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // Values: JSON to Java Object
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Tells JsonDeserializer to trust all packages for deserialization
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Manual Acknowledgment: The consumer must explicitly acknowledge message
        // receipt
        // after successful processing. If it crashes before ack, Kafka redelivers.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // 3 Retries then DLT
        // If a message fails 3 times (with 1s interval), it is sent to the Dead Letter
        // Topic (DLT).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (r, e) -> new org.apache.kafka.common.TopicPartition("myshop.dlt", -1));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3L) // 1 second backoff, 3 retries max
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
