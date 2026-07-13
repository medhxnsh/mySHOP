package com.myshop.service.ai;

import java.util.Locale;

/**
 * Deterministic embedding for tests — no model, no network.
 *
 * Bag-of-words hashing: each token bumps one of 768 buckets, then the vector
 * is L2-normalized. Texts sharing words get high cosine similarity, so
 * "semantic" ranking assertions are deterministic and explainable.
 */
public class FakeEmbeddingClient implements EmbeddingClient {

    public static final int DIMENSION = 768;

    private boolean unavailable = false;

    /** Flip to simulate the provider being down (degradation tests). */
    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    @Override
    public float[] embed(String text) {
        if (unavailable) {
            throw new EmbeddingUnavailableException("fake provider down", null);
        }
        float[] vector = new float[DIMENSION];
        for (String token : text.toLowerCase(Locale.ROOT).split("\\W+")) {
            if (token.isBlank()) {
                continue;
            }
            vector[Math.floorMod(token.hashCode(), DIMENSION)] += 1f;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIMENSION; i++) {
                vector[i] /= (float) norm;
            }
        }
        return vector;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }
}
