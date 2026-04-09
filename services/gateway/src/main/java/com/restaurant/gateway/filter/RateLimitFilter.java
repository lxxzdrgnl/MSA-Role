package com.restaurant.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Rate limiting filter using a sliding window algorithm.
 * Runs before all other filters (Order -1) to reject excessive
 * requests early, before body caching or JWT verification.
 *
 * Limits:
 *   - Auth endpoints (login/register): 10 requests per minute
 *   - All other API endpoints: 60 requests per minute
 *
 * Skipped paths: health checks, Swagger/OpenAPI docs, WebSocket.
 */
@Component
@Order(-1)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final int AUTH_LIMIT = 20;
    private static final int DEFAULT_LIMIT = 300;

    private static final Set<String> AUTH_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/register"
    );

    /**
     * Per-client, per-bucket list of request timestamps within the current window.
     * Key format: "ip:bucket" where bucket is "auth" or "default".
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> requestLog =
            new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();

        // Skip rate limiting for non-API, health, swagger, and websocket paths
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(httpRequest);
        boolean isAuthPath = AUTH_PATHS.contains(path);
        int limit = isAuthPath ? AUTH_LIMIT : DEFAULT_LIMIT;
        String bucket = isAuthPath ? "auth" : "default";
        String key = clientIp + ":" + bucket;

        if (!allowRequest(key, limit)) {
            log.warn("Rate limit exceeded for {} on path {} (bucket={}, limit={})",
                    clientIp, path, bucket, limit);

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("Retry-After", "60");
            httpResponse.getWriter().write(
                "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\","
                + "\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Returns true if the path should bypass rate limiting entirely.
     */
    private boolean shouldSkip(String path) {
        // Health / actuator endpoints
        if (path.startsWith("/actuator") || path.equals("/health")) return true;
        // Swagger / OpenAPI docs
        if (path.contains("swagger") || path.contains("api-docs")) return true;
        // WebSocket
        if (path.startsWith("/ws/")) return true;
        // Internal service-to-service calls
        if (path.startsWith("/internal/")) return true;
        // Non-API static resources
        if (!path.startsWith("/api/")) return true;

        return false;
    }

    /**
     * Resolves the real client IP, respecting X-Forwarded-For when present.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First IP in the chain is the original client
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Sliding window rate check. Returns true if the request is allowed.
     * Evicts timestamps older than the window before counting.
     */
    private boolean allowRequest(String key, int limit) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        CopyOnWriteArrayList<Long> timestamps = requestLog.computeIfAbsent(
                key, k -> new CopyOnWriteArrayList<>());

        // Evict expired entries
        timestamps.removeIf(ts -> ts < windowStart);

        if (timestamps.size() >= limit) {
            return false;
        }

        timestamps.add(now);
        return true;
    }

    /**
     * Periodic cleanup on destroy to avoid memory leaks in long-running instances.
     */
    @Override
    public void destroy() {
        requestLog.clear();
    }
}
