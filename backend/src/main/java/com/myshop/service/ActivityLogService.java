package com.myshop.service;

import com.myshop.constants.KafkaTopics;
import com.myshop.kafka.event.UserActivityEvent;
import com.myshop.model.document.UserActivityLog;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.repository.mongo.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ActivityLogService * Phase 1 logged to console. Phase 3 wires this to
 * MongoDB.
 * The @Async annotation means product views never wait for this log write to
 * complete.
 *
 * Phase 3: Now writing to MongoDB user_activity_logs collection with TTL.
 *
 * Phase 10: also publishes to the user.activity Kafka topic (which existed
 * since Phase 0 but had no producer) — ActivityProfileConsumer turns the
 * stream into personalization profiles. Published fire-and-forget from this
 * async method: analytics events are loss-tolerable (a dropped view nudges a
 * recommendation, it doesn't corrupt money state), so the outbox's crash
 * guarantees — and its DB write on every product view — aren't warranted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Log a user activity event asynchronously.
     * 
     * @param emailOrAnonymous The user email who performed the action (or
     *                         'anonymous')
     * @param action           One of: PRODUCT_VIEWED, PRODUCT_SEARCHED,
     *                         CART_UPDATED, ORDER_PLACED
     * @param entityType       The type of entity affected
     * @param entityId         The ID of the entity
     */
    @Async("analyticsTaskExecutor")
    public void logActivity(String emailOrAnonymous, String action, String entityType, String entityId) {
        UUID userId = null;
        if (emailOrAnonymous != null && !emailOrAnonymous.equals("anonymous")) {
            userId = userRepository.findByEmail(emailOrAnonymous).map(u -> u.getId()).orElse(null);
        }

        UserActivityLog logEntry = UserActivityLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(Map.of("source", "backend_api"))
                .build();

        activityLogRepository.save(logEntry);

        // Phase 10: feed the personalization pipeline. Key = userId (or the
        // entityId for anonymous) so one user's events stay ordered.
        UserActivityEvent event = UserActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .occurredAt(Instant.now())
                .build();
        String key = userId != null ? userId.toString() : entityId;
        kafkaTemplate.send(KafkaTopics.USER_ACTIVITY, key, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.warn("user.activity publish failed (analytics-grade, not retried): {}",
                                ex.getMessage());
                    }
                });

        log.debug("[ASYNC-ANALYTICS] Saved to MongoDB: userId={} action={} entityType={} entityId={}",
                userId, action, entityType, entityId);
    }
}
