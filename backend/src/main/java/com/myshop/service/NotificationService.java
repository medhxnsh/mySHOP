package com.myshop.service;

import com.myshop.dto.response.NotificationResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.model.document.Notification;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.repository.mongo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async("generalTaskExecutor")
    public CompletableFuture<Void> createNotification(UUID userId, String type, String title, String body,
            Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .metadata(metadata)
                .build();

        notificationRepository.save(notification);
        log.info("Notification stored in MongoDB for user: {}", userId);
        return CompletableFuture.completedFuture(null);
    }

    public PagedResponse<NotificationResponse> getUserNotifications(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
                pageable);

        List<NotificationResponse> content = notifications.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.of(notifications, content);
    }

    public NotificationResponse markAsRead(String email, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        // Since MongoRepository doesn't have an update mechanism like JPA @Query,
        // For simplicity in Phase 3 we fetch all unread and save.
        // In real life, use MongoTemplate for bulk updates.
        List<Notification> unread = notificationRepository.findAll().stream()
                .filter(n -> n.getUserId().equals(user.getId()) && !n.getIsRead())
                .toList();

        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getIsRead(),
                notification.getCreatedAt());
    }
}
