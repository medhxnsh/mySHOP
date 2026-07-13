package com.myshop.kafka.consumer;

import com.myshop.constants.KafkaTopics;
import com.myshop.kafka.event.ProductEvent;
import com.myshop.service.ai.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * EmbeddingConsumer — keeps search embeddings in sync with product content.
 *
 * product.updated (via the outbox) → embed name+description → UPDATE
 * products.embedding. Failures are NOT acknowledged: the container's error
 * handler retries 3× and then dead-letters, so a down Ollama never loses an
 * update — it just delays it. Re-processing is idempotent (same text → same
 * vector).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingConsumer {

    private final EmbeddingService embeddingService;

    @KafkaListener(topics = KafkaTopics.PRODUCT_UPDATED, groupId = "embedding-service")
    public void onProductUpdated(@Payload ProductEvent event, Acknowledgment acknowledgment) {
        if (!event.isActive()) {
            // Inactive products are filtered out of search by is_active; the
            // stale vector is harmless and will refresh if reactivated.
            acknowledgment.acknowledge();
            return;
        }
        embeddingService.embedAndStore(event.getProductId(), event.getName(), event.getDescription());
        log.info("Search embedding refreshed for product {} ({})", event.getProductId(), event.getName());
        acknowledgment.acknowledge();
    }
}
