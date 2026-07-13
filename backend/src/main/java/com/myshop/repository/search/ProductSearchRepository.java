package com.myshop.repository.search;

import com.myshop.model.search.ProductSearchHit;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ProductSearchRepository — native SQL for everything pgvector (Phase 8).
 *
 * WHY JdbcTemplate INSTEAD OF THE JPA ENTITY?
 * The embedding and search_tsv columns are search infrastructure, not domain
 * state. Keeping them out of the Product entity means:
 * 1. plain-Postgres test databases (Hibernate create-drop, no pgvector
 *    extension) still work for every non-search test, and
 * 2. no accidental loading of 768-float vectors on every catalog read.
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    /**
     * RRF constant — dampens the head of each ranking so one signal can't
     * dominate; 60 is the value from the original RRF paper and what
     * Elasticsearch/OpenSearch use by default.
     */
    private static final int RRF_K = 60;

    private static final int CANDIDATES_PER_SIGNAL = 50;

    private final JdbcTemplate jdbcTemplate;

    public void updateEmbedding(UUID productId, float[] embedding) {
        jdbcTemplate.update(
                "UPDATE products SET embedding = CAST(? AS vector) WHERE id = ?",
                toVectorLiteral(embedding), productId);
    }

    /**
     * Hybrid search — Reciprocal Rank Fusion of the keyword ranking
     * (tsvector/ts_rank) and the semantic ranking (pgvector cosine).
     * score = Σ 1/(k + rank_i) over the signals that matched.
     */
    public List<ProductSearchHit> hybridSearch(String query, float[] queryEmbedding, int limit, int offset) {
        String sql = """
                WITH kw AS (
                    SELECT id, ROW_NUMBER() OVER (
                               ORDER BY ts_rank(search_tsv, plainto_tsquery('english', ?)) DESC) AS r
                    FROM products
                    WHERE is_active = true AND search_tsv @@ plainto_tsquery('english', ?)
                    LIMIT %d
                ),
                vec AS (
                    SELECT id, ROW_NUMBER() OVER (ORDER BY embedding <=> CAST(? AS vector)) AS r
                    FROM products
                    WHERE is_active = true AND embedding IS NOT NULL
                    LIMIT %d
                )
                SELECT p.id,
                       COALESCE(1.0 / (%d + kw.r), 0) + COALESCE(1.0 / (%d + vec.r), 0) AS score
                FROM products p
                LEFT JOIN kw ON kw.id = p.id
                LEFT JOIN vec ON vec.id = p.id
                WHERE kw.id IS NOT NULL OR vec.id IS NOT NULL
                ORDER BY score DESC, p.id
                LIMIT ? OFFSET ?
                """.formatted(CANDIDATES_PER_SIGNAL, CANDIDATES_PER_SIGNAL, RRF_K, RRF_K);

        return jdbcTemplate.query(sql,
                (rs, i) -> new ProductSearchHit(rs.getObject("id", UUID.class), rs.getDouble("score")),
                query, query, toVectorLiteral(queryEmbedding), limit, offset);
    }

    /** Keyword-only fallback — used when the embedding provider is down. */
    public List<ProductSearchHit> keywordSearch(String query, int limit, int offset) {
        String sql = """
                SELECT id, ts_rank(search_tsv, plainto_tsquery('english', ?)) AS score
                FROM products
                WHERE is_active = true AND search_tsv @@ plainto_tsquery('english', ?)
                ORDER BY score DESC, id
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(sql,
                (rs, i) -> new ProductSearchHit(rs.getObject("id", UUID.class), rs.getDouble("score")),
                query, query, limit, offset);
    }

    /** Nearest neighbours of an existing product's stored vector — no API call. */
    public List<ProductSearchHit> findSimilar(UUID productId, int limit) {
        String sql = """
                SELECT id, 1 - (embedding <=> (SELECT embedding FROM products WHERE id = ?)) AS score
                FROM products
                WHERE is_active = true
                  AND embedding IS NOT NULL
                  AND id <> ?
                  AND (SELECT embedding FROM products WHERE id = ?) IS NOT NULL
                ORDER BY embedding <=> (SELECT embedding FROM products WHERE id = ?)
                LIMIT ?
                """;
        return jdbcTemplate.query(sql,
                (rs, i) -> new ProductSearchHit(rs.getObject("id", UUID.class), rs.getDouble("score")),
                productId, productId, productId, productId, limit);
    }

    /** Products still missing an embedding (backfill worklist). */
    public List<UUID> findIdsMissingEmbedding(int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM products WHERE embedding IS NULL AND is_active = true LIMIT ?",
                UUID.class, limit);
    }

    /** pgvector text literal: "[0.1,0.2,...]" */
    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
