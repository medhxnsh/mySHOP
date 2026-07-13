package com.myshop.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OllamaEmbeddingClient — self-hosted embeddings, zero external cost.
 *
 * Talks to the `ollama` docker-compose service running nomic-embed-text
 * (768-dim). First call after a cold start loads the model into memory
 * (~seconds); subsequent calls are tens of milliseconds on CPU, fine for
 * product-catalog volumes.
 */
@Slf4j
@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

    static final int DIMENSION = 768; // nomic-embed-text output size = vector(768) column

    private final RestClient restClient;
    private final String model;

    public OllamaEmbeddingClient(
            @Value("${myshop.embeddings.base-url:http://localhost:11434}") String baseUrl,
            @Value("${myshop.embeddings.model:nomic-embed-text}") String model) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        // Generous read timeout: the very first request may pull the model
        // into memory. Steady-state responses are far faster.
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/embeddings")
                    .body(Map.of("model", model, "prompt", text))
                    .retrieve()
                    .body(Map.class);

            Object raw = response == null ? null : response.get("embedding");
            if (!(raw instanceof List<?> values) || values.isEmpty()) {
                throw new EmbeddingUnavailableException(
                        "Ollama returned no embedding for model " + model, null);
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = ((Number) values.get(i)).floatValue();
            }
            if (vector.length != DIMENSION) {
                throw new EmbeddingUnavailableException(
                        "Expected " + DIMENSION + " dims from " + model + " but got " + vector.length
                                + " — model/column mismatch",
                        null);
            }
            return vector;
        } catch (EmbeddingUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingUnavailableException("Embedding call to Ollama failed", e);
        }
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }
}
