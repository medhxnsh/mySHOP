package com.myshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TokenRefreshRequest — used to get a new access token using a refresh token.
 *
 * WHY A SEPARATE REFRESH TOKEN?
 * Access tokens are short-lived (15 min) to limit exposure if intercepted.
 * Refresh tokens are long-lived (7 days) but stored securely and can be
 * revoked.
 * This way, the user doesn't need to log in every 15 minutes.
 *
 * FLOW: expired access token → POST /refresh with refresh token → new access
 * token
 */
@Data
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
