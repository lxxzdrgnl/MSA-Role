package com.restaurant.order.controller;

import com.restaurant.order.dto.*;
import com.restaurant.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<OrderResponse> response = orderService.getMyOrders(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        PageResponse<OrderResponse> response = orderService.getAllOrders(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders() {
        List<OrderResponse> response = orderService.getActiveOrders();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            OrderResponse response = orderService.cancelOrder(id, userId);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody OrderStatusRequest request) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        OrderResponse response = orderService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<List<RevenueResponse>> getRevenueStats(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<RevenueResponse> response = orderService.getRevenueStats(period, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/best-sellers")
    public ResponseEntity<List<BestSellerResponse>> getBestSellers(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "10") int limit) {
        List<BestSellerResponse> response = orderService.getBestSellers(period, limit);
        return ResponseEntity.ok(response);
    }
}
