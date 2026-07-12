package com.myshop.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.constants.AppConstants;
import com.myshop.exception.ErrorCode;
import com.myshop.exception.IdempotencyException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private IdempotencyAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new IdempotencyAspect(redisTemplate, objectMapper);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequestWithKey(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (key != null) {
            request.addHeader(AppConstants.IDEMPOTENCY_KEY_HEADER, key);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void missingHeader_isRejectedBeforeExecution() throws Throwable {
        bindRequestWithKey(null);

        assertThatThrownBy(() -> aspect.enforceIdempotency(joinPoint))
                .isInstanceOf(IdempotencyException.class)
                .extracting(e -> ((IdempotencyException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);

        verify(joinPoint, never()).proceed();
    }

    @Test
    void freshKey_executesAndStoresResponse() throws Throwable {
        bindRequestWithKey("key-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("IN_PROGRESS"), any(Duration.class))).thenReturn(true);
        ResponseEntity<Map<String, String>> response = ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("orderId", "o-1"));
        when(joinPoint.proceed()).thenReturn(response);

        Object result = aspect.enforceIdempotency(joinPoint);

        assertThat(result).isSameAs(response);
        // Completed record replaces the claim, with the long TTL.
        verify(valueOps).set(anyString(), org.mockito.ArgumentMatchers.contains("o-1"),
                eq(Duration.ofHours(24)));
    }

    @Test
    void inProgressKey_conflicts() throws Throwable {
        bindRequestWithKey("key-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("IN_PROGRESS"), any(Duration.class))).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn("IN_PROGRESS");

        assertThatThrownBy(() -> aspect.enforceIdempotency(joinPoint))
                .isInstanceOf(IdempotencyException.class)
                .extracting(e -> ((IdempotencyException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(joinPoint, never()).proceed();
    }

    @Test
    void completedKey_replaysStoredResponseWithoutExecuting() throws Throwable {
        bindRequestWithKey("key-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("IN_PROGRESS"), any(Duration.class))).thenReturn(false);
        String storedBody = objectMapper.writeValueAsString(Map.of("orderId", "o-1"));
        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(
                new IdempotencyAspect.StoredResponse(201, storedBody)));

        Object result = aspect.enforceIdempotency(joinPoint);

        verify(joinPoint, never()).proceed();
        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> replayed = (ResponseEntity<?>) result;
        assertThat(replayed.getStatusCode().value()).isEqualTo(201);
        assertThat(((JsonNode) replayed.getBody()).get("orderId").asText()).isEqualTo("o-1");
    }

    @Test
    void failure_releasesClaimSoClientCanRetry() throws Throwable {
        bindRequestWithKey("key-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("IN_PROGRESS"), any(Duration.class))).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("stock conflict"));

        assertThatThrownBy(() -> aspect.enforceIdempotency(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("stock conflict");

        verify(redisTemplate).delete(anyString());
    }
}
