package com.myshop.repository.jpa;

import com.myshop.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Load the next batch of pending events, oldest first.
     *
     * FOR UPDATE SKIP LOCKED: if several application instances run the relay
     * concurrently, each SELECT locks the rows it picked and skips rows locked
     * by other instances — no double-publish, no lock contention. The lock is
     * held until the surrounding transaction commits, which is also when
     * published_at is flushed.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
              AND attempts < 10
            ORDER BY created_at
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findBatchForPublish();

    long countByPublishedAtIsNull();
}
