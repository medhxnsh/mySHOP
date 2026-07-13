-- V8: semantic + keyword product search (Phase 8).
--
-- embedding: 768-dim vector from nomic-embed-text (self-hosted via Ollama),
--            written asynchronously by EmbeddingConsumer off product.updated
--            events. Deliberately NOT mapped as a JPA attribute — search is
--            infrastructure, accessed only through native SQL in
--            repository/search/ProductSearchRepository, which also keeps
--            plain-Postgres test databases (no pgvector) working.
--
-- search_tsv: generated tsvector for the keyword half of hybrid search.
--             Hybrid ranking = Reciprocal Rank Fusion of both signals.

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE products ADD COLUMN embedding vector(768);

ALTER TABLE products ADD COLUMN search_tsv tsvector
    GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))
    ) STORED;

-- HNSW: approximate nearest-neighbour index — kNN stays fast as the catalog grows.
CREATE INDEX idx_products_embedding ON products
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_products_search_tsv ON products USING gin (search_tsv);
