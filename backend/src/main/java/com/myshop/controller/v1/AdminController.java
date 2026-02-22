package com.myshop.controller.v1;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import com.myshop.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** AdminController — Phase 4 Cache Management Implementation. */
@Tag(name = "Admin", description = "Administrative operations")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CacheManager cacheManager;

    @Operation(summary = "Clear a specific cache by name", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/cache/{cacheName}")
    public ResponseEntity<ApiResponse<Void>> clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(com.myshop.exception.ErrorCode.RESOURCE_NOT_FOUND.name(),
                        "Cache not found: " + cacheName));
    }
}
