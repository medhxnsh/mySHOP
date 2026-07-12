package com.myshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the myShop Spring Boot application.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: This class can define Spring beans (via @Bean methods)
 * - @EnableAutoConfiguration: Spring Boot scans dependencies and auto-configures
 *   beans. E.g., it sees PostgreSQL driver on classpath → creates DataSource bean.
 * - @ComponentScan: Scans this package and sub-packages for @Component,
 *   @Service, @Repository, @Controller, etc. and registers them as beans.
 *
 * @EnableAsync: Enables Spring's asynchronous method execution capability.
 * Without this, @Async annotations on methods are silently IGNORED.
 * With this, Spring creates a proxy around @Async methods that submits
 * them to a thread pool instead of running them in the caller's thread.
 *
 * @EnableScheduling: Enables @Scheduled methods — required by OutboxRelay,
 * which polls the transactional outbox and delivers pending events to Kafka.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MyShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyShopApplication.class, args);
    }
}
