package com.myshop.service.ai;

import com.myshop.model.entity.Product;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * One-shot embedding backfill for products created before Phase 8 (or whose
 * events dead-lettered while the embedding provider was down).
 *
 * Enable with MYSHOP_EMBEDDINGS_BACKFILL=true for a single startup, then turn
 * it off — steady-state updates flow through product.updated events. Runs in
 * batches and keeps going past individual failures (they stay NULL and are
 * picked up by the next backfill run).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "myshop.embeddings.backfill", havingValue = "true")
public class EmbeddingBackfillRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 50;

    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Embedding backfill starting...");
        int done = 0;
        int failed = 0;
        while (true) {
            List<UUID> batch = productSearchRepository.findIdsMissingEmbedding(BATCH_SIZE);
            if (batch.isEmpty()) {
                break;
            }
            int batchDone = 0;
            for (UUID id : batch) {
                Product product = productRepository.findById(id).orElse(null);
                if (product == null) {
                    continue;
                }
                try {
                    embeddingService.embedAndStore(id, product.getName(), product.getDescription());
                    done++;
                    batchDone++;
                } catch (EmbeddingUnavailableException e) {
                    failed++;
                    log.warn("Backfill: embedding failed for product {} — leaving NULL ({})", id, e.getMessage());
                }
            }
            // Failed rows stay NULL and would be re-selected next iteration:
            // a batch with zero successes means the provider is down — stop
            // instead of spinning on the same rows forever.
            if (batchDone == 0) {
                log.error("Embedding backfill aborted: provider unavailable ({} failures so far)", failed);
                return;
            }
            log.info("Embedding backfill progress: {} done, {} failed", done, failed);
        }
        log.info("Embedding backfill complete: {} embedded, {} failed", done, failed);
    }
}
