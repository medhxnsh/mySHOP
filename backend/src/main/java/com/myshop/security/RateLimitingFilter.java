package com.myshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.constants.CacheKeys;
import com.myshop.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Global Rate Limiting Filter using Redis (via Redisson).
 *
 * Requirements:
 * 1. 100 requests per minute per IP globally.
 * 2. 5 requests per minute per IP for Auth endpoints (/api/v1/auth/*) to
 * prevent brute force.
 *
 * Ordered before Spring Security filter chain to drop requests early, saving
 * CPU/DB.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Just after RequestIdFilter
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * Limits are configurable (Phase 9): load tests and different deployment
     * sizes shouldn't require a code change. Defaults preserve the original
     * hard-coded values.
     */
    @org.springframework.beans.factory.annotation.Value("${myshop.rate-limit.auth-per-minute}")
    private int authPerMinute;

    @org.springframework.beans.factory.annotation.Value("${myshop.rate-limit.api-per-minute}")
    private int apiPerMinute;

    /**
     * Flash purchases get their own, much larger bucket: thousands of
     * legitimate buyers hit the endpoint in the same seconds (and behind one
     * NAT they share an IP). Abuse is already bounded harder than any IP
     * limit could — the Lua script allows ONE purchase per authenticated user.
     */
    @org.springframework.beans.factory.annotation.Value("${myshop.rate-limit.flash-per-minute}")
    private int flashPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Check Auth specific rate limit (stricter)
        if (path.startsWith("/api/v1/auth/")) {
            if (!isAllowed(CacheKeys.format(CacheKeys.RATE_LIMIT_AUTH, clientIp, getMinuteWindow()), authPerMinute)) {
                sendRateLimitResponse(response, "Too many authentication attempts. Please try again later.");
                return;
            }
        } else if (path.startsWith("/api/v1/flash-sales/")) {
            // Dedicated high-throughput bucket for the flash-sale hot path
            if (!isAllowed(CacheKeys.format(CacheKeys.RATE_LIMIT_FLASH, clientIp, getMinuteWindow()),
                    flashPerMinute)) {
                sendRateLimitResponse(response, "Too many flash-sale requests. Please slow down.");
                return;
            }
        } else if (path.startsWith("/api/")) {
            // Check Global API rate limit
            if (!isAllowed(CacheKeys.format(CacheKeys.RATE_LIMIT_API, clientIp, getMinuteWindow()), apiPerMinute)) {
                sendRateLimitResponse(response, "Too many API requests. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String key, int maxRequestsPerMinute) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // Initialize rate limiter if it doesn't exist
        // RateType.OVERALL means across all instances/threads using this Redis key
        rateLimiter.trySetRate(RateType.OVERALL, maxRequestsPerMinute, 1, RateIntervalUnit.MINUTES);

        // TTL management: if we just created it, set TTL so it doesn't sit in Redis
        // forever
        rateLimiter.expire(java.time.Duration.ofMinutes(2));

        return rateLimiter.tryAcquire(1);
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");

        ApiResponse<Void> apiResponse = ApiResponse.error("rate_limit_exceeded", message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim(); // Get the first IP which is the actual client
    }

    // Returns the current minute window (e.g. seconds since epoch / 60)
    // We use this as part of the key to automatically bucket quotas by minute.
    // Redisson's RRateLimiter tracks rates, but appending minute timestamp to key
    // prevents drift
    // and guarantees reset at the top of the minute, serving as a sliding/fixed
    // window hybrid.
    private long getMinuteWindow() {
        return System.currentTimeMillis() / 60000;
    }
}
