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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
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

    private boolean isPublicPath(String path, String method) {
        // Auth endpoints
        if (SKIP_AUTH_PATHS.contains(path)) return true;
        // Non-API, WebSocket, Swagger
        if (!path.startsWith("/api/") || path.startsWith("/ws/")
                || path.contains("swagger") || path.contains("api-docs")
                || path.startsWith("/internal/")) return true;
        // GET on menus and categories — public read
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/menus")) return true;
            if (path.startsWith("/api/operations/congestion")) return true;
        }
        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        String method = req.getMethod();

        if (isPublicPath(path, method)) {
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
        } catch (jakarta.servlet.ServletException | IOException e) {
            throw e; // Let downstream errors propagate normally
        } catch (Exception e) {
            log.error("Token verification failed for path {}: {}", path, e.getMessage());
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Token verification failed\"}");
        }
    }
}
