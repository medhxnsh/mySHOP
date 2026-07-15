package com.myshop.kafka.consumer;

import com.myshop.constants.KafkaTopics;
import com.myshop.kafka.event.UserActivityEvent;
import com.myshop.service.ai.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * ActivityProfileConsumer — turns the user.activity stream into
 * personalization profiles (Phase 10).
 *
 * Only product interactions by authenticated users move the profile.
 * Re-processing shifts the EMA slightly rather than corrupting it, so
 * at-least-once delivery is acceptable without dedup bookkeeping —
 * a deliberately different idempotency posture than the order pipeline,
 * because the cost of a duplicate here is a nudge, not a double charge.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityProfileConsumer {

    /** Interaction strength: adding to cart says 3× more than a view. */
    private static final Map<String, Float> WEIGHTS = Map.of(
            "PRODUCT_VIEWED", 1f,
            "CART_ADD", 3f);

    private final UserProfileService userProfileService;

    @KafkaListener(topics = KafkaTopics.USER_ACTIVITY, groupId = "profile-service")
    public void onActivity(@Payload UserActivityEvent event, Acknowledgment acknowledgment) {
        Float weight = WEIGHTS.get(event.getAction());
        if (weight != null && event.getUserId() != null && "PRODUCT".equals(event.getEntityType())) {
            userProfileService.applyInteraction(
                    event.getUserId(), UUID.fromString(event.getEntityId()), weight);
            log.debug("Profile updated: user {} {} product {}",
                    event.getUserId(), event.getAction(), event.getEntityId());
        }
        acknowledgment.acknowledge();
    }
}
