package com.myshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * MyShopApplicationTests — Verifies the Spring context loads successfully.
 *
 * @SpringBootTest: Starts the FULL Spring application context.
 *                  If any bean fails to initialize, configuration is wrong, or
 *                  Flyway
 *                  migration fails — this test FAILS. It's the
 *                  canary-in-the-mine test.
 *
 *                  In Phase 0, we use @ActiveProfiles("test") with
 *                  Testcontainers to spin up
 *                  a real PostgreSQL database for this context test.
 */
@SpringBootTest
@ActiveProfiles("test")
class MyShopApplicationTests {

    @Test
    void contextLoads() {
        // If Spring context fails to start, this test fails automatically.
        // No assertions needed — the @SpringBootTest annotation does the work.
    }
}
