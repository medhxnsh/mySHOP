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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Check Auth specific rate limit (stricter)
        if (path.startsWith("/api/v1/auth/")) {
            if (!isAllowed(CacheKeys.format(CacheKeys.RATE_LIMIT_AUTH, clientIp, getMinuteWindow()), 5)) {
                sendRateLimitResponse(response, "Too many authentication attempts. Please try again later.");
                return;
            }
        }

        // Check Global API rate limit
        if (path.startsWith("/api/")) {
            if (!isAllowed(CacheKeys.format(CacheKeys.RATE_LIMIT_API, clientIp, getMinuteWindow()), 100)) {
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
