package com.myshop.service;

import com.myshop.constants.CacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis Distributed Lock Service.
 *
 * WHY NOT JUST USE Java's 'synchronized' KEYWORD?
 * 1. Horizontal Scaling: If we deploy 5 instances of our backend behind a load
 * balancer,
 * 'synchronized' only protects threads inside the single JVM. Two requests
 * hitting
 * Node A and Node B can still execute the "synchronized" block simultaneously,
 * causing race conditions (like overselling stock).
 * 2. Distributed Locks via Redisson fix this by using a centralized Redis
 * server to
 * manage the lock state atomically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private final RedissonClient redissonClient;

    public <T> T executeWithLock(String stockId, long waitTime, long leaseTime, Supplier<T> action) {
        String lockKey = CacheKeys.format(CacheKeys.LOCK_STOCK, stockId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Tries to acquire the lock. Waits up to waitTime. If acquired, sets leaseTime.
            boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (isLocked) {
                log.debug("Lock acquired for {}", lockKey);
                try {
                    return action.get();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock for {}", lockKey);
                throw new IllegalStateException("Could not acquire lock, please try again.");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for lock", e);
        }
    }

    public void executeWithLockVoid(String stockId, long waitTime, long leaseTime, Runnable action) {
        executeWithLock(stockId, waitTime, leaseTime, () -> {
            action.run();
            return null;
        });
    }
}
