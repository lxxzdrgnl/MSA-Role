package com.restaurant.order.service;

import com.restaurant.order.config.AppProperties;
import com.restaurant.order.dto.BestSellerResponse;
import com.restaurant.order.repository.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class BestSellerScheduler {

    private static final Logger log = LoggerFactory.getLogger(BestSellerScheduler.class);

    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public BestSellerScheduler(OrderItemRepository orderItemRepository,
                               RestTemplate restTemplate,
                               AppProperties appProperties) {
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void updateBestSellers() {
        log.info("Running daily best seller update...");
        try {
            List<BestSellerResponse> topSellers = orderItemRepository.getBestSellersForDays(7, 10);

            for (BestSellerResponse seller : topSellers) {
                try {
                    String url = appProperties.getMenuServiceUrl()
                            + "/internal/menus/" + seller.getMenuId() + "/best";
                    restTemplate.patchForObject(url, Map.of("isBest", true), Void.class);
                    log.info("Marked menu {} as best seller", seller.getMenuId());
                } catch (Exception e) {
                    log.warn("Failed to update best seller flag for menu {}: {}",
                            seller.getMenuId(), e.getMessage());
                }
            }

            log.info("Best seller update completed. Updated {} menus.", topSellers.size());
        } catch (Exception e) {
            log.error("Best seller scheduler failed: {}", e.getMessage(), e);
        }
    }
}
