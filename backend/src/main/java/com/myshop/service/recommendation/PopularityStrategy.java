package com.myshop.service.recommendation;

import com.myshop.dto.response.ProductResponse;
import com.myshop.mapper.ProductMapper;
import com.myshop.model.entity.Product;
import com.myshop.repository.jpa.OrderItemRepository;
import com.myshop.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** "Popular now": best sellers by units sold. Serves anonymous + cold-start users. */
@Component
@RequiredArgsConstructor
public class PopularityStrategy implements RecommendationStrategy {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public String name() {
        return "popular";
    }

    @Override
    public List<ProductResponse> recommend(UUID userId, int limit) {
        List<UUID> ids = orderItemRepository.findBestSellingProductIds(PageRequest.of(0, limit));
        Map<UUID, Product> byId = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(productMapper::toResponse)
                .toList();
    }
}
