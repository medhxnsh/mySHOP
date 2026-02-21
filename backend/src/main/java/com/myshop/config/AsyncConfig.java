package com.myshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AsyncConfig — Defines named thread pool beans for @Async methods.
 *
 * WHY DO WE NEED CUSTOM THREAD POOLS?
 * By default, Spring uses SimpleAsyncTaskExecutor for @Async methods.
 * This creates a NEW THREAD for every single async call — catastrophic
 * under load. If 10,000 requests hit simultaneously, 10,000 threads
 * are created. Each thread uses ~1MB of memory = 10GB RAM consumed,
 * plus massive CPU context switching overhead.
 *
 * With a ThreadPool, we create a fixed set of threads upfront and reuse them.
 * Excess tasks wait in a queue. This is bounded, predictable, and efficient.
 *
 * WHY MULTIPLE POOLS?
 * Different tasks have different priorities and resource needs.
 * - General tasks (email, notifications): moderate volume, user-facing
 * - Analytics tasks: high volume, non-critical, can be slow
 * Separating them prevents a flood of analytics tasks from blocking
 * the more important general tasks.
 */
@Configuration
public class AsyncConfig {

    /**
     * General-purpose async thread pool.
     * Used for: email simulation, notification sending, moderate async tasks.
     *
     * Thread pool sizing for I/O-bound tasks (MAANG interview answer):
     * Rule of thumb: N_cores * 2 for I/O-bound (threads spend time waiting for I/O)
     * vs N_cores + 1 for CPU-bound (threads always computing, extra 1 for
     * preemption gaps)
     *
     * Why 2 core / 5 max?
     * This is a development configuration. In production (Phase 7), sizes would be
     * tuned based on actual load metrics from Grafana.
     */
    @Bean("generalTaskExecutor")
    public ThreadPoolTaskExecutor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // corePoolSize: Always keep this many threads alive, even when idle.
        // They're ready to pick up tasks immediately without thread creation overhead.
        executor.setCorePoolSize(2);

        // maxPoolSize: When queue is FULL, create additional threads up to this limit.
        // IMPORTANT: New threads are only created when the queue is full,
        // not when the queue has items. This is a common gotcha.
        executor.setMaxPoolSize(5);

        // queueCapacity: Tasks wait here when all core threads are busy.
        // If queue fills AND maxPoolSize is reached → RejectedExecutionException
        // unless we set a rejection policy (see below).
        executor.setQueueCapacity(100);

        // threadNamePrefix: Names threads for easier debugging.
        // When you look at a thread dump, you'll see "async-general-1", not
        // "pool-3-thread-7"
        executor.setThreadNamePrefix("async-general-");

        // waitForTasksToCompleteOnShutdown: When app shuts down, don't kill threads
        // mid-task. Wait for queued tasks to finish. Important for data integrity.
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // awaitTerminationSeconds: But don't wait forever. After 30s, force shutdown.
        executor.setAwaitTerminationSeconds(30);

        // CallerRunsPolicy: When pool is full (queue + max threads exhausted),
        // instead of throwing an exception, the CALLER'S THREAD runs the task.
        // This is "backpressure" — it slows the caller down, preventing overload.
        // Alternative policies: AbortPolicy (throw exception) or DiscardPolicy
        // (silently drop)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * Analytics/logging thread pool.
     * Used for: user activity logging, view tracking, search analytics.
     * High volume (every page view generates a log), non-critical (losing a few is
     * okay).
     *
     * Larger queue (500) means we can absorb bursts without creating extra threads.
     * More core threads (4) because analytics events are frequent.
     */
    @Bean("analyticsTaskExecutor")
    public ThreadPoolTaskExecutor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-analytics-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        // Analytics are non-critical: CallerRunsPolicy still provides backpressure
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
