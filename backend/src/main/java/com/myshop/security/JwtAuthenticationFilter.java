package com.myshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter — runs on EVERY request, once per request.
 *
 * FILTER CHAIN MENTAL MODEL:
 * Browser → [RequestIdFilter] → [JwtAuthenticationFilter] → [SecurityFilter] →
 * [Controller]
 *
 * This filter does the following on each request:
 * 1. Extract the Authorization header
 * 2. Parse the Bearer token
 * 3. Validate the token (signature + expiry)
 * 4. Load user from DB (or UserDetails cache)
 * 5. Set SecurityContextHolder (marks request as authenticated)
 * 6. Continue the filter chain (no short-circuit on success)
 *
 * WHY OncePerRequestFilter?
 * Some frameworks call filters multiple times per request (e.g. on
 * forwards/includes).
 * OncePerRequestFilter guarantees it runs exactly once no matter what.
 *
 * WHAT HAPPENS IF TOKEN IS MISSING/INVALID?
 * We don't throw an exception — we simply don't set the SecurityContext.
 * The request continues. Spring Security's next filter
 * (ExceptionTranslationFilter)
 * checks if the secured endpoint requires auth → returns 401 if it does.
 * For public endpoints, no auth needed → request proceeds normally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Step 1 & 2: Extract Bearer token from header
            String jwt = extractTokenFromRequest(request);

            // Step 3: Validate token (null check + signature + expiry)
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {

                // Step 4: Extract email and load UserDetails
                String email = jwtTokenProvider.extractEmail(jwt);
                if (email != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (userDetails != null) {
                        /**
                         * Step 5: Create authentication token.
                         * UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                         * - principal: who is authenticated (UserDetails)
                         * - credentials: null (we don't need the password after successful auth)
                         * - authorities: granted roles (ROLE_USER, ROLE_ADMIN)
                         *
                         * The 3-arg constructor marks the authentication as "authenticated=true".
                         * The 2-arg constructor (without authorities) marks it as
                         * "authenticated=false".
                         */
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        // Attaches request details (IP, session ID) to the authentication object
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        /**
                         * Step 5b: Set in SecurityContextHolder.
                         * SecurityContextHolder is thread-local — each thread (request) has its own
                         * context.
                         * After this line, Spring Security considers this request authenticated.
                         * SecurityUtils.getCurrentUserEmail() reads from here.
                         */
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            // Don't propagate — unauthenticated state is handled downstream
            log.debug("Could not set user authentication: {}", e.getMessage());
        }

        // ALWAYS continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract token from "Authorization: Bearer <token>" header.
     * Returns null if header is missing or malformed.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix (7 chars)
        }
        return null;
    }
}
