package com.myshop.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.constants.AppConstants;
import com.myshop.constants.CacheKeys;
import com.myshop.exception.IdempotencyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * IdempotencyAspect — makes @Idempotent endpoints safely retryable.
 *
 * PROTOCOL (per user + Idempotency-Key header):
 * 1. Claim the key with SET NX. Fresh key → run the controller method.
 * 2. Success → overwrite the claim with the serialized response (24h TTL).
 *    A retry with the same key replays that stored response byte-for-byte
 *    without re-executing the operation (no duplicate order).
 * 3. Failure → the claim is deleted so the client may retry cleanly.
 * 4. Key currently IN_PROGRESS (double-click racing itself) → 409.
 *
 * The IN_PROGRESS claim gets a short TTL: if the process dies mid-request the
 * key self-heals instead of blocking the user for a day.
 *
 * WHY AN ASPECT AND NOT CODE IN OrderService?
 * Single Responsibility — the service stays pure business logic; idempotency
 * is transport-level plumbing. Any future endpoint becomes idempotent by
 * adding @Idempotent, with zero service changes.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private static final String STATE_IN_PROGRESS = "IN_PROGRESS";
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(60);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final int MAX_KEY_LENGTH = 128;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.myshop.util.Idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        String clientKey = currentRequestHeader();
        if (clientKey == null || clientKey.isBlank()) {
            throw IdempotencyException.missingKey();
        }
        if (clientKey.length() > MAX_KEY_LENGTH) {
            throw IdempotencyException.invalidKey("must be at most " + MAX_KEY_LENGTH + " characters");
        }

        // Scoped per user so one client can never replay (or block) another
        // user's key. Anonymous endpoints would share the "anonymous" scope —
        // acceptable because @Idempotent is only used on authenticated routes.
        String user = SecurityUtils.getCurrentUserEmail().orElse("anonymous");
        String redisKey = CacheKeys.format(CacheKeys.IDEMPOTENCY, user, clientKey);

        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, STATE_IN_PROGRESS, IN_PROGRESS_TTL);

        if (!Boolean.TRUE.equals(claimed)) {
            String stored = redisTemplate.opsForValue().get(redisKey);
            if (stored == null || STATE_IN_PROGRESS.equals(stored)) {
                // Another request with this key is in flight (or just expired
                // mid-race) — the client should wait and retry.
                throw IdempotencyException.conflict();
            }
            StoredResponse replay = objectMapper.readValue(stored, StoredResponse.class);
            // JsonNode is serialized verbatim by Jackson, so the client gets a
            // byte-identical copy of the original response body.
            JsonNode body = objectMapper.readTree(replay.body());
            log.info("Idempotent replay for user={}, key={} (original status {})",
                    user, clientKey, replay.status());
            return ResponseEntity.status(replay.status()).body(body);
        }

        try {
            Object result = joinPoint.proceed();
            if (!(result instanceof ResponseEntity<?> response)) {
                // Configuration error, not a runtime condition — fail loudly.
                throw new IllegalStateException("@Idempotent methods must return ResponseEntity, but "
                        + joinPoint.getSignature() + " returned "
                        + (result == null ? "null" : result.getClass().getName()));
            }
            String bodyJson = objectMapper.writeValueAsString(response.getBody());
            String record = objectMapper.writeValueAsString(
                    new StoredResponse(response.getStatusCode().value(), bodyJson));
            redisTemplate.opsForValue().set(redisKey, record, COMPLETED_TTL);
            return result;
        } catch (Throwable t) {
            // Failed attempts must not poison the key — release the claim so
            // the client can retry with the same Idempotency-Key.
            redisTemplate.delete(redisKey);
            throw t;
        }
    }

    private String currentRequestHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(AppConstants.IDEMPOTENCY_KEY_HEADER);
    }

    /** What we persist in Redis for completed requests. */
    record StoredResponse(int status, String body) {
    }
}
