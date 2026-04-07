package com.restaurant.order.controller;

import com.restaurant.order.dto.ActiveCountResponse;
import com.restaurant.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns order history for the last N days.
     * Includes menu-level order counts and hourly order patterns.
     * Called by AI Operations Service.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getOrderHistory(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> response = orderService.getOrderHistory(days);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the count of currently active (PENDING + COOKING) orders.
     * Called by AI Operations Service for congestion analysis.
     */
    @GetMapping("/active-count")
    public ResponseEntity<ActiveCountResponse> getActiveCount() {
        ActiveCountResponse response = orderService.getActiveCount();
        return ResponseEntity.ok(response);
    }
}
