package com.myshop.service.recommendation;

import com.myshop.dto.response.ProductResponse;
import com.myshop.mapper.ProductMapper;
import com.myshop.repository.jpa.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Last resort: newest active products (an empty shop still renders a rail). */
@Component
@RequiredArgsConstructor
public class NewestStrategy implements RecommendationStrategy {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public String name() {
        return "newest";
    }

    @Override
    public List<ProductResponse> recommend(UUID userId, int limit) {
        return productRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .filter(p -> p.isActive())
                .map(productMapper::toResponse)
                .toList();
    }
}
