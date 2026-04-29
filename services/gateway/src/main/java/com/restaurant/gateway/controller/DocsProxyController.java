package com.restaurant.gateway.controller;

import com.restaurant.gateway.config.RouteConfig;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/docs")
public class DocsProxyController {

    private final RestTemplate restTemplate;
    private final RouteConfig routeConfig;

    // Spring Boot uses /v3/api-docs, FastAPI uses /openapi.json
    private static final String SPRING_DOCS_PATH = "/v3/api-docs";
    private static final String FASTAPI_DOCS_PATH = "/openapi.json";

    public DocsProxyController(RestTemplate restTemplate, RouteConfig routeConfig) {
        this.restTemplate = restTemplate;
        this.routeConfig = routeConfig;
    }

    @GetMapping("/{service}/v3/api-docs")
    public ResponseEntity<String> getApiDocs(@PathVariable String service) {
        String baseUrl = resolveServiceUrl(service);
        if (baseUrl == null) {
            return ResponseEntity.notFound().build();
        }

        String docsPath = isFastApi(service) ? FASTAPI_DOCS_PATH : SPRING_DOCS_PATH;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + docsPath, String.class);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"" + service + " service unavailable\"}");
        }
    }

    private boolean isFastApi(String service) {
        return service.startsWith("ai-");
    }

    private String resolveServiceUrl(String service) {
        return switch (service) {
            case "auth" -> routeConfig.getAuthUrl();
            case "menu" -> routeConfig.getMenuUrl();
            case "order" -> routeConfig.getOrderUrl();
            case "review" -> routeConfig.getReviewUrl();
            case "ai-recommendation" -> routeConfig.getAiRecommendationUrl();
            case "ai-review-writer" -> routeConfig.getAiReviewWriterUrl();
            case "ai-operations" -> routeConfig.getAiOperationsUrl();
            case "ai-validation" -> routeConfig.getAiValidationUrl();
            default -> null;
        };
    }
}
