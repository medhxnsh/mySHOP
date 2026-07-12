package com.myshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.dto.request.LoginRequest;
import com.myshop.dto.request.RegisterRequest;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Auth API integration tests.
 *
 * Self-contained: the "test" profile runs against a fresh Testcontainers
 * database with Flyway disabled, so no V2 seed data exists — every fixture
 * user is created here. Rate-limit counters live in the real local Redis and
 * survive across runs, so they are cleared before each test (the auth bucket
 * allows only 5 requests/minute per IP — without clearing, back-to-back runs
 * 429 spuriously).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    private static final String SEED_EMAIL = "seeded-user@example.com";
    private static final String SEED_PASSWORD = "User@123";
    private static final String NEW_EMAIL = "newuser@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        clearRateLimitCounters();
        if (userRepository.findByEmail(SEED_EMAIL).isEmpty()) {
            userRepository.save(User.builder()
                    .email(SEED_EMAIL)
                    .passwordHash(passwordEncoder.encode(SEED_PASSWORD))
                    .fullName("Seeded User")
                    .role("USER")
                    .isActive(true)
                    .build());
        }
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(NEW_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(SEED_EMAIL).ifPresent(userRepository::delete);
    }

    private void clearRateLimitCounters() {
        Set<String> keys = redisTemplate.keys("rate_limit:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail(NEW_EMAIL);
        request.setPassword("StrongPass1!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                // The refresh token is deliberately NOT in the body — it travels
                // as an httpOnly cookie scoped to /api/v1/auth/refresh.
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.data.user.email").value(NEW_EMAIL));

        assertTrue(userRepository.findByEmail(NEW_EMAIL).isPresent());
    }

    @Test
    void register_DuplicateEmail_Returns422() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail(SEED_EMAIL); // created in setUp()
        request.setPassword("StrongPass1!");

        // BusinessException(EMAIL_ALREADY_EXISTS) maps to 422 Unprocessable
        // Entity in GlobalExceptionHandler (semantically valid request that
        // violates a business rule — not a malformed 400).
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(SEED_EMAIL);
        request.setPassword(SEED_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(SEED_EMAIL));
    }

    @Test
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(SEED_EMAIL);
        request.setPassword("WrongPassword1!");

        // BadCredentialsException maps to 401 with code UNAUTHORIZED — the
        // response body intentionally does not reveal whether the email or the
        // password was wrong.
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
