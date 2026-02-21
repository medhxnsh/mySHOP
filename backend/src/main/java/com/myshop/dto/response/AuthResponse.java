package com.myshop.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * AuthResponse — returned after successful login or registration.
 *
 * WHY BOTH ACCESS TOKEN AND REFRESH TOKEN?
 * - accessToken: short-lived (15 min), sent in every API request header
 * - refreshToken: long-lived (7 days), used only to get new access tokens
 * - tokenType: always "Bearer" — tells the client how to send the token
 * - expiresIn: milliseconds until access token expires (for client-side timers)
 *
 * The client should NOT store accessToken in localStorage (XSS risk).
 * Best practice: store in memory (Redux/Zustand state).
 * RefreshToken may be stored in an HttpOnly cookie for security.
 * For simplicity, we keep both returned in the response instead of using
 * cookies.
 * body.
 */
@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Time until access token expires, in milliseconds */
    private long expiresIn;

    /** The authenticated user's basic info */
    private UserResponse user;
}
