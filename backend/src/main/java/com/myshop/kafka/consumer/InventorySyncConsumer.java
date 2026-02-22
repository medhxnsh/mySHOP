package com.myshop.kafka.consumer;

import com.myshop.kafka.event.InventoryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventorySyncConsumer {

    @KafkaListener(topics = "inventory.updated", groupId = "inventory-sync-service")
    public void consumeInventoryUpdatedEvent(@Payload InventoryEvent event, Acknowledgment acknowledgment) {
        // Just log for now. In Phase 6, this will synchronize the state with
        // Elasticsearch.
        log.info("InventorySyncConsumer received: Product {} stock changed from {} to {} due to {}",
                event.getProductId(), event.getOldQuantity(), event.getNewQuantity(), event.getReason());

        acknowledgment.acknowledge();
    }
}
