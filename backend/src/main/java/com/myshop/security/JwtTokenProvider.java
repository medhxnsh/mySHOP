package com.myshop.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtTokenProvider — creates and validates JSON Web Tokens.
 *
 * HOW JWT WORKS:
 * A JWT has 3 parts separated by dots: header.payload.signature
 *
 * Header: {"alg": "HS256", "typ": "JWT"} → Base64 encoded
 * Payload: {"sub": "email", "iat": ..., "exp": ...} → Base64 encoded (NOT
 * encrypted!)
 * Signature: HMAC_SHA256(base64(header) + "." + base64(payload), secretKey)
 *
 * WHY IS THE SIGNATURE IMPORTANT?
 * Anyone can decode the payload (it's just Base64). But only the server
 * can CREATE a valid signature because only the server knows the secretKey.
 * If an attacker modifies the payload, the signature won't match → rejected.
 *
 * WHY HS256 (symmetric) not RS256 (asymmetric)?
 * HS256: same key signs AND verifies. Simpler. Fine for monoliths.
 * RS256: private key signs, public key verifies. Better for microservices
 * (each service has the public key but not the private key).
 * We'll use HS256 here (Phase 0 → monolith).
 *
 * WHY @Component?
 * Registered as a Spring bean so it can be @Autowired into filters and
 * services.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long accessTokenExpiryMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshTokenExpiryMs) {

        // Keys.hmacShaKeyFor: creates a secure key from the secret bytes
        // Validates key length (≥32 bytes for HS256)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /**
     * Generate an ACCESS token for the given user.
     * Access tokens are short-lived (15 min). They contain the user's email
     * so the server can identify the user without hitting the database on every
     * request.
     *
     * Claims included:
     * - sub: user's email (subject, used to load user on each request)
     * - iat: issued-at timestamp
     * - exp: expiry timestamp
     */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), accessTokenExpiryMs);
    }

    /**
     * Generate a REFRESH token.
     * Longer TTL (7 days). Contains only the email — minimal claims
     * because refresh tokens are only used to issue new access tokens.
     */
    public String generateRefreshToken(String email) {
        return buildToken(email, refreshTokenExpiryMs);
    }

    private String buildToken(String subject, long expiryMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract the email (subject) from a token.
     * No validation here — call validateToken first.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validate a token: checks signature + expiry.
     *
     * EXCEPTION HIERARCHY:
     * - ExpiredJwtException: token has passed its exp time
     * - UnsupportedJwtException: token format is wrong
     * - MalformedJwtException: token can't be parsed
     * - SecurityException: signature validation failed
     * - IllegalArgumentException: token is null/empty
     *
     * All → return false → filter won't set SecurityContext → 401 Unauthorized.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("JWT token unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("JWT token malformed: {}", e.getMessage());
        } catch (JwtException e) {
            // Catches SignatureException, InvalidClaimException, and other jjwt errors
            log.debug("JWT validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT token is null or empty: {}", e.getMessage());
        }
        return false;
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
