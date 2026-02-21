package com.myshop.controller.v1;

import com.myshop.dto.request.LoginRequest;
import com.myshop.dto.request.RegisterRequest;
import com.myshop.dto.request.TokenRefreshRequest;
import com.myshop.dto.response.ApiResponse;
import com.myshop.dto.response.AuthResponse;
import com.myshop.dto.response.UserResponse;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.mapper.UserMapper;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.UserRepository;
import com.myshop.service.AuthService;
import com.myshop.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — handles registration, login, token refresh, and current user
 * info.
 *
 * @Tag: Swagger UI groups all endpoints from this class under "Authentication".
 * @Operation: Documents each endpoint in Swagger UI.
 *             @SecurityRequirement("bearerAuth"): Tells Swagger to show the
 *             Bearer token lock icon.
 *
 *             RESPONSE STRATEGY:
 *             Every endpoint returns ApiResponse<T> — our consistent JSON
 *             envelope.
 *             Success: { "success": true, "data": {...}, "requestId": "...",
 *             "timestamp": "..." }
 *             Error: { "success": false, "error": {...}, "requestId": "...",
 *             "timestamp": "..." }
 *
 *             This consistency means the frontend knows exactly how to parse
 *             every response
 *             regardless of which endpoint was called.
 */
@Tag(name = "Authentication", description = "Register, login, token refresh, and current user")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Operation(summary = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);
        // 201 Created — a new resource (user) was created
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);
        // 200 OK — authentication is an action, not resource creation
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @Operation(summary = "Refresh access token using a refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {

        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @Operation(summary = "Get currently authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        // SecurityUtils reads from SecurityContextHolder (set by
        // JwtAuthenticationFilter)
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));

        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(user)));
    }
}
