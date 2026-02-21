package com.myshop.service;

import com.myshop.dto.request.LoginRequest;
import com.myshop.dto.request.RegisterRequest;
import com.myshop.dto.request.TokenRefreshRequest;
import com.myshop.dto.response.AuthResponse;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.mapper.UserMapper;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService — handles registration, login, and token refresh.
 *
 * KEY PATTERNS:
 *
 * 1. @Transactional on register:
 * If the save succeeds but something fails after (unlikely here but if we
 * send a welcome email and that crashes), the transaction rolls back.
 * User won't be partially registered. Atomicity.
 *
 * 2. AuthenticationManager for login — WHY NOT check password manually?
 * AuthenticationManager.authenticate() does:
 * a) calls userDetailsService.loadUserByUsername(email)
 * b) calls passwordEncoder.matches(rawPassword, storedHash)
 * c) checks account active, not locked, not expired
 * Manual password checking would skip steps b and c — subtle bugs.
 * Always delegate to Spring Security's auth manager.
 *
 * 3. Token refresh — the refresh token is validated but we don't store
 * refresh tokens in DB (Phase 0 simplicity). Production would:
 * - Store refresh token hash in DB
 * - Check if it's been revoked on logout
 * - Implement token rotation (issue new refresh token on each refresh)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final UserMapper userMapper;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                // Idempotency check: prevent duplicate email registrations
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS,
                                        "Email already registered: " + request.getEmail());
                }

                // Build the user entity
                User user = User.builder()
                                .email(request.getEmail().toLowerCase().trim())
                                // encode() uses BCrypt with cost factor 10
                                // NEVER store raw password
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName().trim())
                                .role("USER") // new registrations always get USER role
                                .isActive(true)
                                .build();

                User savedUser = userRepository.save(user);
                log.info("New user registered: {}", savedUser.getEmail());

                // Generate tokens immediately after registration — user is logged in
                UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
                String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .expiresIn(jwtTokenProvider.getAccessTokenExpiryMs())
                                .user(userMapper.toResponse(savedUser))
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                /**
                 * authenticate() internals:
                 * 1. Loads user by email via UserDetailsServiceImpl
                 * 2. BCrypt.matches(request.password, storedHash)
                 * 3. Checks isEnabled, isAccountNonExpired, isAccountNonLocked
                 * Throws BadCredentialsException if any check fails → 401 via
                 * GlobalExceptionHandler
                 */
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
                // TODO Phase 4: move refresh token to httpOnly cookie backed by Redis
                String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUsername());

                User user = userRepository.findByEmail(userDetails.getUsername())
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));

                log.info("User logged in: {}", userDetails.getUsername());

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .expiresIn(jwtTokenProvider.getAccessTokenExpiryMs())
                                .user(userMapper.toResponse(user))
                                .build();
        }

        public AuthResponse refreshToken(TokenRefreshRequest request) {
                String refreshToken = request.getRefreshToken();

                if (!jwtTokenProvider.validateToken(refreshToken)) {
                        throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token is invalid or expired");
                }

                String email = jwtTokenProvider.extractEmail(refreshToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));

                return AuthResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(refreshToken) // same refresh token (no rotation in Phase 1)
                                .expiresIn(jwtTokenProvider.getAccessTokenExpiryMs())
                                .user(userMapper.toResponse(user))
                                .build();
        }
}
