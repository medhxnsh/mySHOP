package com.myshop.repository.mongo;

import com.myshop.model.document.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @org.springframework.data.mongodb.repository.Query(value = "{ 'metadata.orderId': ?0, 'type': ?1 }", exists = true)
    boolean existsByMetadataOrderIdAndType(String orderId, String type);
}
