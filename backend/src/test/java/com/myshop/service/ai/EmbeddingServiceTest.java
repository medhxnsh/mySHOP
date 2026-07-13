package com.myshop.service.ai;

import com.myshop.repository.search.ProductSearchRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    private final FakeEmbeddingClient fakeClient = new FakeEmbeddingClient();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new EmbeddingService(fakeClient, productSearchRepository, meterRegistry);
    }

    @Test
    void embedAndStore_writesNormalizedVector() {
        UUID productId = UUID.randomUUID();

        service.embedAndStore(productId, "Warm Jacket", "cozy winter jacket");

        verify(productSearchRepository).updateEmbedding(eq(productId), any(float[].class));
    }

    @Test
    void embedAndStore_providerDown_countsFailureAndRethrows() {
        fakeClient.setUnavailable(true);

        assertThatThrownBy(() -> service.embedAndStore(UUID.randomUUID(), "x", "y"))
                .isInstanceOf(EmbeddingUnavailableException.class);

        verify(productSearchRepository, never()).updateEmbedding(any(), any());
        assertThat(meterRegistry.counter("myshop.embedding.failures").count()).isEqualTo(1.0);
    }

    @Test
    void fakeEmbeddings_sharedWordsMeanHigherCosine() {
        float[] jacket = fakeClient.embed("warm winter jacket");
        float[] similar = fakeClient.embed("warm jacket for winter hiking");
        float[] unrelated = fakeClient.embed("usb cable adapter electronics");

        assertThat(cosine(jacket, similar)).isGreaterThan(cosine(jacket, unrelated));
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // both vectors are already L2-normalized
    }
}
