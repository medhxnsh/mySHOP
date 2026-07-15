package com.myshop.controller.v1;

import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.RecommendationResponse;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.service.recommendation.RecommendationService;
import com.myshop.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Recommendations", description = "Personalized (event-stream profiles) with popularity/newest fallbacks")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @Operation(summary = "Recommendations for the current visitor", description = "Authenticated users with ≥3 product interactions get profile-vector kNN "
            + "('personalized'); everyone else gets best sellers ('popular') or newest products.")
    @GetMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> recommendations(
            @RequestParam(defaultValue = "8") int limit) {
        UUID userId = SecurityUtils.getCurrentUserEmail()
                .flatMap(userRepository::findByEmail)
                .map(u -> u.getId())
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.recommend(userId, Math.min(limit, 24))));
    }
}
