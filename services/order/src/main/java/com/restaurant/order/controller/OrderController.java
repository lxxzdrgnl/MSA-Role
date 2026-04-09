package com.restaurant.order.controller;

import com.restaurant.order.dto.*;
import com.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Order", description = "주문 관리 API")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "주문 생성", description = "새 주문을 생성합니다. X-User-Id 헤더 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "주문 생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 오류 또는 품절 메뉴",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/orders\",\"status\":400,\"code\":\"BAD_REQUEST\",\"message\":\"Menu item is sold out: 비빔밥\"}"))),
        @ApiResponse(responseCode = "401", description = "헤더 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        if (size > 100) size = 100;
        PageResponse<OrderResponse> response = orderService.getMyOrders(userId, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("관리자 권한이 필요합니다.");
        }
        if (size > 100) size = 100;
        PageResponse<OrderResponse> response = orderService.getAllOrders(page, size, status, dateFrom, dateTo, sort);
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

    @Operation(summary = "주문 취소", description = "PENDING 상태 주문만 취소 가능합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "취소 성공"),
        @ApiResponse(responseCode = "403", description = "취소 권한 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/orders/1/cancel\",\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"주문 취소 권한이 없습니다.\"}"))),
        @ApiResponse(responseCode = "404", description = "주문 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 처리된 주문",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/orders/1/cancel\",\"status\":409,\"code\":\"STATE_CONFLICT\",\"message\":\"주문이 이미 처리 중이거나 완료되어 취소할 수 없습니다.\"}")))
    })
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

    @Operation(summary = "주문 상태 변경 (관리자)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 상태값",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "관리자 권한 필요",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "주문 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
