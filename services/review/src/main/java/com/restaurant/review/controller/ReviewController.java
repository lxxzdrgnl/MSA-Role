package com.restaurant.review.controller;

import com.restaurant.review.dto.*;
import com.restaurant.review.service.ReviewService;
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

@Tag(name = "Review", description = "리뷰 관리 API")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "리뷰 작성", description = "주문에 대한 리뷰를 작성합니다. X-User-Id 헤더 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "리뷰 작성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/reviews\",\"status\":400,\"code\":\"VALIDATION_FAILED\",\"message\":\"입력값 검증에 실패했습니다.\",\"details\":{\"rating\":\"rating must be between 1 and 5\"}}"))),
        @ApiResponse(responseCode = "401", description = "헤더 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(userId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(reviewService.getReviews(menuId, rating, page, size, sort));
    }

    @Operation(summary = "리뷰 상세 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "리뷰 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/reviews/1\",\"status\":404,\"code\":\"RESOURCE_NOT_FOUND\",\"message\":\"Review not found with id: 1\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @Operation(summary = "리뷰 삭제", description = "본인 리뷰 또는 ADMIN만 삭제 가능합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "헤더 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/reviews/1\",\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"You do not have permission to delete this review\"}"))),
        @ApiResponse(responseCode = "404", description = "리뷰 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        reviewService.deleteReview(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<ReviewSummaryResponse> getSummary(
            @RequestParam(required = false) Long menuId) {
        if (menuId != null) {
            return ResponseEntity.ok(reviewService.getMenuSummary(menuId));
        }
        return ResponseEntity.ok(reviewService.getSummary());
    }

    @GetMapping("/by-order")
    public ResponseEntity<PageResponse<ReviewResponse>> getByOrder(
            @RequestParam Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(reviewService.getReviewsByOrder(orderId, page, size, sort));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<ReviewResponse>> getMyReviews(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId, page, size, sort));
    }

    @GetMapping("/ai-summary")
    public ResponseEntity<java.util.Map<String, Object>> getAiSummary(@RequestParam Long menuId) {
        return ResponseEntity.ok(reviewService.getAiReviewSummary(menuId));
    }

    @PostMapping("/generate")
    public ResponseEntity<AiGenerateResponse> generateReview(
            @Valid @RequestBody AiGenerateRequest request) {
        return ResponseEntity.ok(reviewService.generateAiReview(request));
    }
}
