package com.restaurant.review.service;

import com.restaurant.review.dto.*;
import com.restaurant.review.entity.Review;
import com.restaurant.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.review-writer.url}")
    private String aiReviewWriterUrl;

    @Value("${ai.recommendation.url}")
    private String aiRecommendationUrl;

    @Value("${ai.validation.url}")
    private String aiValidationUrl;

    public ReviewService(ReviewRepository reviewRepository, RestTemplate restTemplate) {
        this.reviewRepository = reviewRepository;
        this.restTemplate = restTemplate;
    }

    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        Review review = new Review(
                userId,
                request.getOrderId(),
                request.getMenuId(),
                request.getMenuName(),
                request.getRating(),
                request.getContent(),
                request.getIsAiGenerated() != null ? request.getIsAiGenerated() : 0
        );
        Review saved = reviewRepository.save(review);

        // trigger async embedding update
        syncEmbeddingAsync(saved.getMenuId());

        return ReviewResponse.from(saved);
    }

    @Async("asyncExecutor")
    public void syncEmbeddingAsync(Long menuId) {
        try {
            // get all reviews for this menu
            List<Review> menuReviews = reviewRepository.findRecentByMenuId(menuId, 100);
            if (menuReviews.isEmpty()) {
                return;
            }

            // build review texts for summarization
            List<Map<String, Object>> reviewData = menuReviews.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("content", r.getContent());
                map.put("rating", r.getRating());
                map.put("menu_name", r.getMenuName());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> summarizeRequest = new HashMap<>();
            summarizeRequest.put("menu_id", menuId);
            summarizeRequest.put("menu_name", menuReviews.get(0).getMenuName());
            summarizeRequest.put("reviews", reviewData);

            // call AI Review Writer to summarize
            Map<String, Object> summaryResponse = restTemplate.postForObject(
                    aiReviewWriterUrl + "/reviews/summarize",
                    summarizeRequest,
                    Map.class
            );

            if (summaryResponse != null && summaryResponse.containsKey("summary")) {
                // call AI Recommendation to sync embedding
                Map<String, Object> embeddingRequest = new HashMap<>();
                embeddingRequest.put("menu_id", menuId);
                embeddingRequest.put("review_summary", summaryResponse.get("summary"));

                restTemplate.postForObject(
                        aiRecommendationUrl + "/embeddings/sync",
                        embeddingRequest,
                        Map.class
                );
            }

            log.info("Successfully synced embedding for menu {}", menuId);
        } catch (Exception e) {
            log.warn("Failed to sync embedding for menu {}: {}", menuId, e.getMessage());
        }
    }

    public PageResponse<ReviewResponse> getReviews(Long menuId, int page, int size) {
        int offset = (page - 1) * size;
        List<Review> reviews;
        long totalCount;

        if (menuId != null) {
            reviews = reviewRepository.findByMenuId(menuId, offset, size);
            totalCount = reviewRepository.countByMenuId(menuId);
        } else {
            reviews = reviewRepository.findAll(offset, size);
            totalCount = reviewRepository.countAll();
        }

        List<ReviewResponse> content = reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalCount / size);

        return new PageResponse<>(content, page, totalPages, totalCount);
    }

    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));
        return ReviewResponse.from(review);
    }

    public void deleteReview(Long id, Long userId, String userRole) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + id));

        if (!"ADMIN".equals(userRole) && !review.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to delete this review");
        }

        reviewRepository.deleteById(id);
    }

    public ReviewSummaryResponse getSummary() {
        double averageRating = reviewRepository.getAverageRating();
        long totalCount = reviewRepository.getTotalCount();
        Map<Integer, Long> distribution = buildDistribution(reviewRepository.getRatingDistribution());

        return new ReviewSummaryResponse(
                Math.round(averageRating * 10.0) / 10.0,
                totalCount,
                distribution
        );
    }

    public ReviewSummaryResponse getMenuSummary(Long menuId) {
        double averageRating = reviewRepository.getAverageRatingByMenuId(menuId);
        long totalCount = reviewRepository.getTotalCountByMenuId(menuId);
        Map<Integer, Long> distribution = buildDistribution(reviewRepository.getRatingDistributionByMenuId(menuId));

        return new ReviewSummaryResponse(
                Math.round(averageRating * 10.0) / 10.0,
                totalCount,
                distribution
        );
    }

    private Map<Integer, Long> buildDistribution(List<Map<String, Object>> raw) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (Map<String, Object> row : raw) {
            Integer rating = ((Number) row.get("rating")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            distribution.put(rating, count);
        }
        return distribution;
    }

    public AiGenerateResponse generateAiReview(AiGenerateRequest request) {
        try {
            // step 1: validate input via AI Validation
            Map<String, Object> validationInput = new HashMap<>();
            validationInput.put("text", request.getMenuName() + " " +
                    (request.getKeywords() != null ? String.join(", ", request.getKeywords()) : ""));
            validationInput.put("type", "review_generate");

            Map<String, Object> inputValidation = restTemplate.postForObject(
                    aiValidationUrl + "/validation/input",
                    validationInput,
                    Map.class
            );

            if (inputValidation != null) {
                Boolean isValid = (Boolean) inputValidation.get("is_valid");
                if (isValid != null && !isValid) {
                    String reason = (String) inputValidation.getOrDefault("reason", "Input validation failed");
                    throw new IllegalArgumentException(reason);
                }
            }

            // step 2: call AI Review Writer to generate draft
            Map<String, Object> generateRequest = new HashMap<>();
            generateRequest.put("menu_name", request.getMenuName());
            generateRequest.put("rating", request.getRating());
            generateRequest.put("keywords", request.getKeywords());

            Map<String, Object> generateResponse = restTemplate.postForObject(
                    aiReviewWriterUrl + "/reviews/generate",
                    generateRequest,
                    Map.class
            );

            if (generateResponse == null) {
                return new AiGenerateResponse("맛있게 잘 먹었습니다.", request.getRating());
            }

            String draft = (String) generateResponse.getOrDefault("draft", "맛있게 잘 먹었습니다.");
            Integer rating = generateResponse.get("rating") != null
                    ? ((Number) generateResponse.get("rating")).intValue()
                    : request.getRating();

            // step 3: validate output via AI Validation
            Map<String, Object> validationOutput = new HashMap<>();
            validationOutput.put("text", draft);
            validationOutput.put("type", "review_generate");
            validationOutput.put("context", request.getMenuName());

            Map<String, Object> outputValidation = restTemplate.postForObject(
                    aiValidationUrl + "/validation/output",
                    validationOutput,
                    Map.class
            );

            if (outputValidation != null) {
                Boolean isValid = (Boolean) outputValidation.get("is_valid");
                if (isValid != null && !isValid) {
                    return new AiGenerateResponse("맛있게 잘 먹었습니다.", request.getRating());
                }
            }

            return new AiGenerateResponse(draft, rating);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI review generation failed: {}", e.getMessage());
            return new AiGenerateResponse("맛있게 잘 먹었습니다.", request.getRating());
        }
    }

    public AiAnalyzeResponse analyzeReviews() {
        try {
            // get recent reviews
            List<Review> recentReviews = reviewRepository.findRecent(50);
            if (recentReviews.isEmpty()) {
                return new AiAnalyzeResponse("No reviews to analyze.", null);
            }

            List<Map<String, Object>> reviewData = recentReviews.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("content", r.getContent());
                map.put("rating", r.getRating());
                map.put("menu_name", r.getMenuName());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> analyzeRequest = new HashMap<>();
            analyzeRequest.put("reviews", reviewData);

            Map<String, Object> response = restTemplate.postForObject(
                    aiReviewWriterUrl + "/reviews/analyze",
                    analyzeRequest,
                    Map.class
            );

            if (response == null) {
                return new AiAnalyzeResponse("분석을 일시적으로 이용할 수 없습니다.", null);
            }

            String summary = (String) response.getOrDefault("summary", "분석을 일시적으로 이용할 수 없습니다.");
            Object details = response.get("details");

            return new AiAnalyzeResponse(summary, details);
        } catch (Exception e) {
            log.warn("AI review analysis failed: {}", e.getMessage());
            return new AiAnalyzeResponse("분석을 일시적으로 이용할 수 없습니다.", null);
        }
    }
}
