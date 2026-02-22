package com.myshop.kafka.producer;

import com.myshop.kafka.event.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "inventory.updated";

    public void publishInventoryUpdated(InventoryEvent event) {
        String key = event.getProductId().toString();
        log.info("Producing InventoryEvent to topic {} with key {}: {}", TOPIC, key, event);
        kafkaTemplate.send(TOPIC, key, event);
    }
}
