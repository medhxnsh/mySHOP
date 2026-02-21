package com.myshop.service;

import com.myshop.model.document.UserActivityLog;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.repository.mongo.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * ActivityLogService * Phase 1 logged to console. Phase 3 wires this to
 * MongoDB.
 * The @Async annotation means product views never wait for this log write to
 * complete.
 *
 * Phase 3: Now writing to MongoDB user_activity_logs collection with TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

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

        log.debug("[ASYNC-ANALYTICS] Saved to MongoDB: userId={} action={} entityType={} entityId={}",
                userId, action, entityType, entityId);
    }
}
