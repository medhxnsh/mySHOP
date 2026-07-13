package com.myshop.service;

import com.myshop.config.MetricsConfig;
import com.myshop.constants.CacheKeys;
import com.myshop.constants.KafkaTopics;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.kafka.event.FlashOrderEvent;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FlashPurchaseService — the flash-sale HOT PATH (Phase 9).
 *
 * DESIGN (deliberate deviation from the Phase 6 outbox):
 * This path touches ONLY Redis and Kafka — never Postgres. During a spike,
 * the database is the bottleneck; here Redis is the source of truth for stock
 * and dedup (atomically, via Lua), and order creation happens asynchronously
 * in FlashOrderWorker. There is no DB transaction on this path, so there is
 * nothing for an outbox row to be atomic WITH — instead:
 *   accept (Lua) → publish reservation asynchronously
 *   → on publish failure: compensate (Lua returns the stock unit + buyer slot)
 * If the process dies between accept and publish, the reconciliation job
 * surfaces the drift (buyers set larger than reservations) for repair.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashPurchaseService {

    private static final DefaultRedisScript<Long> PURCHASE_SCRIPT = script("lua/flash_purchase.lua");
    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = script("lua/flash_compensate.lua");

    private static final long ACCEPTED_MIN = 0;
    private static final long SOLD_OUT = -1;
    private static final long NOT_ACTIVE = -2;
    private static final long ALREADY_BOUGHT = -3;

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    private static DefaultRedisScript<Long> script(String classpath) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(classpath)));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * @return the reservation id the client polls while the worker persists
     *         the order.
     */
    @Timed(value = MetricsConfig.FLASH_PURCHASE_TIMER, histogram = true)
    public UUID purchase(UUID saleId, UUID userId) {
        String stockKey = CacheKeys.format(CacheKeys.FLASH_STOCK, saleId);
        String buyersKey = CacheKeys.format(CacheKeys.FLASH_BUYERS, saleId);

        Long outcome = redisTemplate.execute(PURCHASE_SCRIPT,
                List.of(stockKey, buyersKey), userId.toString());

        if (outcome == null || outcome == NOT_ACTIVE) {
            reject("not_active");
            throw new BusinessException(ErrorCode.FLASH_SALE_NOT_ACTIVE,
                    ErrorCode.FLASH_SALE_NOT_ACTIVE.getDefaultMessage());
        }
        if (outcome == ALREADY_BOUGHT) {
            reject("already_bought");
            throw new BusinessException(ErrorCode.FLASH_SALE_ALREADY_PURCHASED,
                    ErrorCode.FLASH_SALE_ALREADY_PURCHASED.getDefaultMessage());
        }
        if (outcome == SOLD_OUT) {
            reject("sold_out");
            throw new BusinessException(ErrorCode.FLASH_SALE_SOLD_OUT,
                    ErrorCode.FLASH_SALE_SOLD_OUT.getDefaultMessage());
        }

        // Accepted — hand the reservation to Kafka. Sale metadata comes from
        // Redis (written at activation) so this path stays DB-free.
        UUID reservationId = UUID.randomUUID();
        Map<Object, Object> meta = redisTemplate.opsForHash()
                .entries(CacheKeys.format(CacheKeys.FLASH_META, saleId));
        FlashOrderEvent event = FlashOrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .reservationId(reservationId)
                .saleId(saleId)
                .userId(userId)
                .productId(UUID.fromString((String) meta.get("productId")))
                .salePrice(new BigDecimal((String) meta.get("salePrice")))
                .build();

        // ASYNC hand-off: blocking on the broker ack here serialized the whole
        // burst behind Kafka round-trips (measured: p99 1.01s at 500 concurrent
        // buyers; ~180ms after this change). The 202 means "accepted, pending
        // confirmation" — the client polls the reservation. If the publish
        // fails, compensation returns the stock unit + buyer slot atomically,
        // the reservation never materializes, and the buyer's poll times out
        // into a retry with their slot restored.
        //
        // send() can ALSO throw synchronously (broker unreachable at metadata
        // time — observed live with Kafka paused): that path must compensate
        // too, or the buyer gets an error WHILE their slot stays consumed.
        try {
            kafkaTemplate.send(KafkaTopics.ORDER_FLASH_REQUESTED, saleId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            compensate(stockKey, buyersKey, userId, reservationId, saleId, ex);
                        }
                    });
        } catch (Exception e) {
            compensate(stockKey, buyersKey, userId, reservationId, saleId, e);
            throw new BusinessException(ErrorCode.FLASH_SALE_INVALID_STATE,
                    "Could not process your purchase right now. Please try again.");
        }
        meterRegistry.counter(MetricsConfig.FLASH_ACCEPTED).increment();
        log.debug("Flash reservation {} accepted (sale {}, user {}, remaining {})",
                reservationId, saleId, userId, outcome);
        return reservationId;
    }

    private void compensate(String stockKey, String buyersKey, UUID userId,
            UUID reservationId, UUID saleId, Throwable cause) {
        redisTemplate.execute(COMPENSATE_SCRIPT, List.of(stockKey, buyersKey), userId.toString());
        reject("handoff_failed");
        log.error("Flash reservation {} hand-off failed (sale {}, user {}) — compensated",
                reservationId, saleId, userId, cause);
    }

    private void reject(String reason) {
        meterRegistry.counter(MetricsConfig.FLASH_REJECTED, "reason", reason).increment();
    }
}
