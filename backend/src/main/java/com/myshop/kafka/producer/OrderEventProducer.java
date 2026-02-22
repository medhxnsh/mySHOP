package com.myshop.kafka.producer;

import com.myshop.kafka.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "order.placed";

    public void publishOrderPlaced(OrderEvent event) {
        String key = event.getUserId().toString();
        log.info("Producing OrderEvent to topic {} with key {}: {}", TOPIC, key, event);
        kafkaTemplate.send(TOPIC, key, event);
    }
}
