package com.myshop.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Configure Spring Cache abstraction backed by Redis.
 * Sets specific TTLs per cache name to void staling while protecting DB.
 */
@Configuration
@EnableCaching
public class CacheConfig {

        public static final String CACHE_PRODUCTS = "products";
        public static final String CACHE_PRODUCTS_PAGED = "products_paged";
        public static final String CACHE_CATEGORIES = "categories";

        @Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.cache.CacheManager cacheManager(
                        org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory) {
                
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                
                com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator ptv = 
                        com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build();
                objectMapper.activateDefaultTyping(ptv, com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL, com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

                org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer serializer = 
                        new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(objectMapper);

                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                        org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(serializer);

                return org.springframework.data.redis.cache.RedisCacheManager.builder(redisConnectionFactory)
                                .initialCacheNames(java.util.Set.of(CACHE_PRODUCTS, CACHE_PRODUCTS_PAGED,
                                                CACHE_CATEGORIES))
                                .withCacheConfiguration(CACHE_PRODUCTS,
                                                org.springframework.data.redis.cache.RedisCacheConfiguration
                                                                .defaultCacheConfig()
                                                                .entryTtl(Duration.ofMinutes(10))
                                                                .serializeValuesWith(jsonSerializer)
                                                                .disableCachingNullValues())
                                .withCacheConfiguration(CACHE_PRODUCTS_PAGED,
                                                org.springframework.data.redis.cache.RedisCacheConfiguration
                                                                .defaultCacheConfig()
                                                                .entryTtl(Duration.ofMinutes(5))
                                                                .serializeValuesWith(jsonSerializer)
                                                                .disableCachingNullValues())
                                .withCacheConfiguration(CACHE_CATEGORIES,
                                                org.springframework.data.redis.cache.RedisCacheConfiguration
                                                                .defaultCacheConfig()
                                                                .entryTtl(Duration.ofHours(1))
                                                                .serializeValuesWith(jsonSerializer)
                                                                .disableCachingNullValues())
                                .build();
        }
}
