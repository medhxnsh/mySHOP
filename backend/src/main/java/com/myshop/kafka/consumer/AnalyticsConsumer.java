package com.myshop.kafka.consumer;

import com.myshop.kafka.event.OrderEvent;
import com.myshop.model.document.AnalyticsRecord;
import com.myshop.repository.mongo.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsRepository analyticsRepository;

    @KafkaListener(topics = "order.placed", groupId = "analytics-service")
    public void consumeOrderPlacedForAnalytics(@Payload OrderEvent event, Acknowledgment acknowledgment) {
        log.info("AnalyticsConsumer received OrderEvent: {}", event.getOrderId());

        AnalyticsRecord record = AnalyticsRecord.builder()
                .eventType("ORDER_PLACED")
                .userId(event.getUserId().toString())
                .data(Map.of(
                        "orderId", event.getOrderId().toString(),
                        "amount", event.getTotalAmount()))
                .build();

        analyticsRepository.save(record);

        acknowledgment.acknowledge();
    }
}
