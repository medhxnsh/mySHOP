package com.myshop.service.ai;

import com.myshop.config.MetricsConfig;
import com.myshop.repository.search.ProductSearchRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * EmbeddingService — embeds product text and stores the vector.
 * Shared by EmbeddingConsumer (event-driven refresh) and
 * EmbeddingBackfillRunner (one-shot catch-up), so the "what text do we embed"
 * decision lives in exactly one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final ProductSearchRepository productSearchRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Idempotent: re-embedding the same content overwrites with the same
     * vector, so at-least-once delivery from Kafka is harmless.
     *
     * @throws EmbeddingUnavailableException bubbled up on provider failure —
     *         the Kafka consumer lets it trigger retry/DLT rather than
     *         swallowing it.
     */
    public void embedAndStore(UUID productId, String name, String description) {
        String text = buildEmbeddingText(name, description);
        try {
            float[] vector = embeddingClient.embed(text);
            productSearchRepository.updateEmbedding(productId, vector);
            log.debug("Embedding stored for product {} ({} dims)", productId, vector.length);
        } catch (EmbeddingUnavailableException e) {
            meterRegistry.counter(MetricsConfig.EMBEDDING_FAILURES).increment();
            throw e;
        }
    }

    /** Query-side embedding (search); same failure accounting. */
    public float[] embedQuery(String query) {
        try {
            return embeddingClient.embed(query);
        } catch (EmbeddingUnavailableException e) {
            meterRegistry.counter(MetricsConfig.EMBEDDING_FAILURES).increment();
            throw e;
        }
    }

    private String buildEmbeddingText(String name, String description) {
        StringBuilder sb = new StringBuilder(name == null ? "" : name);
        if (description != null && !description.isBlank()) {
            sb.append(". ").append(description);
        }
        return sb.toString();
    }
}
