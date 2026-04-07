package com.restaurant.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RouteConfig {

    @Value("${services.auth-url}")
    private String authUrl;

    @Value("${services.menu-url}")
    private String menuUrl;

    @Value("${services.order-url}")
    private String orderUrl;

    @Value("${services.review-url}")
    private String reviewUrl;

    @Value("${services.ai-recommendation-url}")
    private String aiRecommendationUrl;

    @Value("${services.ai-operations-url}")
    private String aiOperationsUrl;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    public String getAuthUrl() { return authUrl; }
    public String getMenuUrl() { return menuUrl; }
    public String getOrderUrl() { return orderUrl; }
    public String getReviewUrl() { return reviewUrl; }

    public String resolveServiceUrl(String path) {
        if (path.startsWith("/api/auth")) return authUrl;
        if (path.startsWith("/api/menus")) return menuUrl;
        if (path.startsWith("/api/orders")) return orderUrl;
        if (path.startsWith("/api/reviews")) return reviewUrl;
        if (path.startsWith("/api/recommendations")) return aiRecommendationUrl;
        if (path.startsWith("/api/operations")) return aiOperationsUrl;
        return null;
    }
}
