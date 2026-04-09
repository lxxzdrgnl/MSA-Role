package com.restaurant.review.controller;

import com.restaurant.review.dto.*;
import com.restaurant.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

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
