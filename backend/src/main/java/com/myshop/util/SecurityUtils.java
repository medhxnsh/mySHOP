package com.myshop.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * SecurityUtils — Utility methods for accessing the current authenticated user.
 *
 * WHY A UTILITY CLASS?
 * The SecurityContextHolder is a ThreadLocal that Spring Security populates
 * after JWT authentication. Without this utility, every service that needs
 * the current user's ID would duplicate the same boilerplate lookup code.
 *
 * IMPORTANT: These methods only work inside an HTTP request thread.
 * 
 * @Async methods running in a different thread WON'T have the security context
 *        unless you configure
 *        SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)
 *        (We handle this in AsyncConfig in Phase 4 when needed).
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class — prevent instantiation
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the email (username) of the currently authenticated user.
     * Returns empty if the user is not authenticated (anonymous request).
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        if (principal instanceof String s) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    /**
     * Returns true if there is an authenticated user in the current context.
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}
