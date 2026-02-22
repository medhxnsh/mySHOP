package com.myshop.kafka.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DLTHandler {

    /**
     * Consumes messages from the Dead Letter Topic.
     * These messages failed to process 3 times in their original consumer.
     */
    @KafkaListener(topics = "myshop.dlt", groupId = "dlt-group")
    public void processDltMessage(@Payload Object payload) {
        log.error("Received message in DLT! Payload: {}", payload);
        // In a real production system, you might save this to a database table
        // or send an alert to PagerDuty/Slack for manual intervention.
    }
}
