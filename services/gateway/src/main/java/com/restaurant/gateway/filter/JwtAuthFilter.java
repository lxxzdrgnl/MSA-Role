package com.restaurant.gateway.filter;

import com.restaurant.gateway.config.RouteConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    private final RestTemplate restTemplate;
    private final RouteConfig routeConfig;

    private static final Set<String> SKIP_AUTH_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh"
    );

    public JwtAuthFilter(RestTemplate restTemplate, RouteConfig routeConfig) {
        this.restTemplate = restTemplate;
        this.routeConfig = routeConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // Skip auth for public endpoints and non-API paths
        if (SKIP_AUTH_PATHS.contains(path) || path.startsWith("/ws/")
                || path.contains("swagger") || path.contains("api-docs")
                || path.startsWith("/internal/")) {
            chain.doFilter(request, response);
            return;
        }

        // Skip auth for non-API paths
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Token is required\"}");
            return;
        }

        String token = authHeader.substring(7);

        // Verify token via Auth Service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{\"token\":\"" + token + "\"}", headers);

            ResponseEntity<Map> verifyResponse = restTemplate.exchange(
                routeConfig.getAuthUrl() + "/api/auth/verify",
                HttpMethod.POST, entity, Map.class
            );

            Map body = verifyResponse.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("valid"))) {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid token\"}");
                return;
            }

            // Add user info headers for downstream services
            String userId = String.valueOf(body.get("userId"));
            String role = String.valueOf(body.get("role"));

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(req) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equals(name)) return userId;
                    if ("X-User-Role".equals(name)) return role;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    names.add("X-User-Id");
                    names.add("X-User-Role");
                    return Collections.enumeration(names);
                }
            };

            chain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Token verification failed\"}");
        }
    }
}
