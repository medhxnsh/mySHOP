package com.myshop.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * RequestIdFilter — Assigns a unique correlation ID to every HTTP request.
 *
 * HOW IT WORKS:
 * 1. Check if the client sent an X-Request-ID header (some clients do for
 * tracing)
 * 2. If yes, use it. If no, generate a new UUID.
 * 3. Store it in SLF4J MDC (Mapped Diagnostic Context)
 * 4. Add it to the response headers so the client can correlate request ↔
 * response
 * 5. ALWAYS clear MDC after the request — otherwise thread pool threads reuse
 * the same MDC context from a previous request (memory leak + wrong data)
 *
 * WHAT IS MDC?
 * MDC is a thread-local map that SLF4J automatically includes in every log
 * statement.
 * Once you do MDC.put("requestId", id), every log line in the request's thread
 * automatically includes "requestId=abc-123" without you passing it around.
 *
 * WHY IS THIS IMPORTANT FOR PRODUCTION?
 * Without request IDs, log files look like:
 * [INFO] Product found: laptop-pro
 * [INFO] Product found: mouse-pad
 * [ERROR] Order failed: INSUFFICIENT_STOCK
 * Whose order? For which product? Impossible to trace.
 *
 * With request IDs:
 * [INFO] [requestId=abc] Product found: laptop-pro
 * [INFO] [requestId=def] Product found: mouse-pad
 * [ERROR] [requestId=def] Order failed: INSUFFICIENT_STOCK
 * Now you can grep for requestId=def and see the full story.
 *
 * OncePerRequestFilter: guarantees this filter runs exactly once per request,
 * even if the request is forwarded internally (Spring's default guarantees
 * this).
 *
 * @Order(1): Run this filter FIRST — before Spring Security, before all others.
 * We want requestId in the MDC before any logging happens downstream.
 */
@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    // The header name — clients can pass their own request ID for distributed
    // tracing
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    // The MDC key — match the pattern in logback configuration
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Check if client provided a request ID (supports distributed tracing
        // where the frontend or API gateway generates the parent trace ID)
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        // If not provided, generate a new UUID
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        try {
            // Store in MDC — now available in ALL log statements in this thread
            MDC.put(REQUEST_ID_MDC_KEY, requestId);

            // Add to response header — frontend can correlate its network tab entry
            // to this specific server log line
            response.addHeader(REQUEST_ID_HEADER, requestId);

            // Continue the filter chain — all subsequent processing has MDC set
            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: Clear MDC when request is done.
            // Thread pool threads are REUSED. Without this, the next request
            // on this thread would log with the PREVIOUS request's ID — corrupted tracing.
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
