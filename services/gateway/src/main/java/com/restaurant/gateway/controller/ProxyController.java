package com.restaurant.gateway.controller;

import com.restaurant.gateway.config.RouteConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;

@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final RestTemplate restTemplate;
    private final RouteConfig routeConfig;

    public ProxyController(RestTemplate restTemplate, RouteConfig routeConfig) {
        this.restTemplate = restTemplate;
        this.routeConfig = routeConfig;
    }

    @RequestMapping(
        value = {"/api/**", "/images/**"},
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE}
    )
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetBase = routeConfig.resolveServiceUrl(path);

        if (targetBase == null) {
            return ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"Service not found\"}".getBytes());
        }

        String targetUrlStr = targetBase + path + (query != null ? "?" + query : "");
        URI targetUrl = URI.create(targetUrlStr);

        String method = request.getMethod();
        long startTime = System.currentTimeMillis();

        // Copy all request headers (except host/content-length which RestTemplate manages)
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String lower = name.toLowerCase();
            if (!lower.equals("host") && !lower.equals("content-length")) {
                headers.set(name, request.getHeader(name));
            }
        }

        // Read raw body bytes — works for JSON, multipart, form-urlencoded, empty
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        HttpEntity<byte[]> entity = new HttpEntity<>(bodyBytes.length > 0 ? bodyBytes : null, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                targetUrl, HttpMethod.valueOf(method), entity, byte[].class);

            long elapsed = System.currentTimeMillis() - startTime;
            int status = response.getStatusCode().value();

            if (status >= 400) {
                log.warn("PROXY {} {} -> {} [{}] {}ms", method, path, targetUrlStr, status, elapsed);
            } else {
                log.info("PROXY {} {} -> {} [{}] {}ms", method, path, targetUrlStr, status, elapsed);
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("Transfer-Encoding")) {
                    responseHeaders.put(key, values);
                }
            });

            return ResponseEntity.status(response.getStatusCode())
                .headers(responseHeaders)
                .body(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            int status = e.getStatusCode().value();
            log.warn("PROXY {} {} -> {} [{}] {}ms", method, path, targetUrlStr, status, elapsed);

            return ResponseEntity.status(e.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.getResponseBodyAsByteArray());
        }
    }
}
