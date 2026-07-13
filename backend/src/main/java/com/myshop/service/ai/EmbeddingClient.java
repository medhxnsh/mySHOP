package com.myshop.service.ai;

/**
 * EmbeddingClient — port for text-to-vector embedding (Phase 8).
 *
 * Business code (EmbeddingService, ProductSearchService) depends on this
 * interface, never on a concrete provider (DIP). Implementations:
 * - OllamaEmbeddingClient: self-hosted nomic-embed-text, zero cost (production)
 * - FakeEmbeddingClient (test sources): deterministic vectors — integration
 *   tests never need a model running
 *
 * Swapping to a paid API (Voyage/OpenAI) later = one new adapter class,
 * a dimension change in V-next, and zero caller changes.
 */
public interface EmbeddingClient {

    /**
     * Embed a piece of text.
     *
     * @throws EmbeddingUnavailableException when the provider cannot be
     *         reached — callers decide whether to degrade (search falls back
     *         to keyword-only) or retry later (consumer lets the message go
     *         through Kafka retry/DLT).
     */
    float[] embed(String text);

    /** Vector dimensionality — must match the products.embedding column. */
    int dimension();
}
