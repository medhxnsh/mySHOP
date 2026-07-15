package com.myshop.service.recommendation;

import com.myshop.dto.response.ProductResponse;
import com.myshop.mapper.ProductMapper;
import com.myshop.model.document.UserProfile;
import com.myshop.model.entity.Product;
import com.myshop.model.search.ProductSearchHit;
import com.myshop.repository.jpa.OrderItemRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.search.ProductSearchRepository;
import com.myshop.service.ai.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Recommended for you": pgvector kNN around the user's profile vector
 * (the EMA of embeddings of products they viewed/carted), excluding what
 * they already bought.
 */
@Component
@RequiredArgsConstructor
public class PersonalizedStrategy implements RecommendationStrategy {

    /** Below this many interactions a profile is noise, not taste. */
    static final int MIN_EVENTS = 3;

    private final UserProfileService userProfileService;
    private final ProductSearchRepository productSearchRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public String name() {
        return "personalized";
    }

    @Override
    public List<ProductResponse> recommend(UUID userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        UserProfile profile = userProfileService.find(userId).orElse(null);
        if (profile == null || profile.getEventCount() < MIN_EVENTS) {
            return List.of(); // cold start — let the chain fall through
        }

        List<UUID> purchased = orderItemRepository.findPurchasedProductIds(userId);
        List<ProductSearchHit> hits = productSearchRepository.findNearestToVector(
                UserProfileService.toArray(profile.getVector()), purchased, limit);

        List<UUID> orderedIds = hits.stream().map(ProductSearchHit::productId).toList();
        Map<UUID, Product> byId = productRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return orderedIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(productMapper::toResponse)
                .toList();
    }
}
