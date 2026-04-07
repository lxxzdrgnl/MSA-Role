package com.restaurant.gateway.controller;

import com.restaurant.gateway.config.RouteConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;

@RestController
public class ProxyController {

    private final RestTemplate restTemplate;
    private final RouteConfig routeConfig;

    public ProxyController(RestTemplate restTemplate, RouteConfig routeConfig) {
        this.restTemplate = restTemplate;
        this.routeConfig = routeConfig;
    }

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<String> proxy(HttpServletRequest request, @RequestBody(required = false) String body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetBase = routeConfig.resolveServiceUrl(path);

        if (targetBase == null) {
            return ResponseEntity.status(404).body("{\"error\":\"Service not found\"}");
        }

        String targetUrl = targetBase + path + (query != null ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                headers.set(name, request.getHeader(name));
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl, HttpMethod.valueOf(request.getMethod()), entity, String.class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("Transfer-Encoding")) {
                    responseHeaders.put(key, values);
                }
            });

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
