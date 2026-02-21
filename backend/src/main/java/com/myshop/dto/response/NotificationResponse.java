package com.myshop.dto.response;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String body,
        Boolean isRead,
        Instant createdAt) {
}
