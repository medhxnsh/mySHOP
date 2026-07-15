package com.myshop.service.ai;

import com.myshop.model.document.UserProfile;
import com.myshop.repository.mongo.UserProfileRepository;
import com.myshop.repository.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserProfileService — maintains per-user taste vectors (Phase 10).
 *
 * profile ← normalize( (1-α)·profile + α·weight·productVector )
 *
 * The exponential moving average favors recent interest without forgetting
 * history; keeping the vector normalized makes it directly usable as a
 * pgvector cosine query. Weights: view=1, cart-add=3 — adding to cart is a
 * far stronger signal than a click.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    /** EMA step — one interaction shifts the profile 20% toward that product. */
    private static final float ALPHA = 0.2f;

    private final UserProfileRepository userProfileRepository;
    private final ProductSearchRepository productSearchRepository;

    public void applyInteraction(UUID userId, UUID productId, float weight) {
        float[] productVector = productSearchRepository.getEmbedding(productId);
        if (productVector == null) {
            // Product not embedded yet — skip rather than poison the profile.
            log.debug("No embedding for product {} — interaction not applied to profile", productId);
            return;
        }

        UserProfile profile = userProfileRepository.findById(userId.toString())
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId.toString())
                        .vector(zeroes(productVector.length))
                        .eventCount(0)
                        .build());

        List<Float> current = profile.getVector();
        float[] updated = new float[productVector.length];
        for (int i = 0; i < productVector.length; i++) {
            float existing = i < current.size() ? current.get(i) : 0f;
            updated[i] = (1 - ALPHA) * existing + ALPHA * weight * productVector[i];
        }
        normalize(updated);

        List<Float> boxed = new ArrayList<>(updated.length);
        for (float v : updated) {
            boxed.add(v);
        }
        profile.setVector(boxed);
        profile.setEventCount(profile.getEventCount() + 1);
        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);
    }

    public Optional<UserProfile> find(UUID userId) {
        return userProfileRepository.findById(userId.toString());
    }

    public static float[] toArray(List<Float> vector) {
        float[] array = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            array[i] = vector.get(i);
        }
        return array;
    }

    private static List<Float> zeroes(int n) {
        List<Float> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(0f);
        }
        return list;
    }

    private static void normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }
    }
}
