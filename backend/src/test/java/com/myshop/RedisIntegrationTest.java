package com.myshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "integration.tests", matches = "true")
public class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        // Using ephemeral redis without auth for testcontainer simplicity
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private org.redisson.api.RedissonClient redissonClient;

    @Test
    void testRedisCachingIsConfigured() {
        assertNotNull(cacheManager, "CacheManager should be present in context");
        assertNotNull(cacheManager.getCache("products"), "Products cache should be configured");
    }

    @Test
    void testRedissonIsConnected() {
        assertNotNull(redissonClient, "RedissonClient should be present in context");
        assertTrue(redissonClient.getNodesGroup().pingAll(), "Redisson should be able to ping the Redis container");
    }
}
