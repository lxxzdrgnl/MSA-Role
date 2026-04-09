package com.restaurant.review.service;

import com.restaurant.review.dto.*;
import com.restaurant.review.entity.Review;
import com.restaurant.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${ai.review-writer.url}")
    private String aiReviewWriterUrl;

    @Value("${ai.recommendation.url}")
    private String aiRecommendationUrl;

    @Value("${ai.validation.url}")
    private String aiValidationUrl;

    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String CACHE_PREFIX = "review:summary:";

    public ReviewService(ReviewRepository reviewRepository, RestTemplate restTemplate,
                         StringRedisTemplate redisTemplate) {
        this.reviewRepository = reviewRepository;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
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

    @SuppressWarnings("unchecked")
    private Map<Long, String> fetchNicknames(List<ReviewResponse> responses) {
        try {
            List<Long> userIds = responses.stream()
                    .map(ReviewResponse::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) return Collections.emptyMap();
            Map<String, List<Long>> body = Map.of("userIds", userIds);
            // Auth 서비스 내부 API 직접 호출
            Map<?, ?> result = restTemplate.postForObject(
                    authServiceUrl + "/api/auth/nicknames", userIds, Map.class);
            if (result == null) return Collections.emptyMap();
            Map<Long, String> map = new HashMap<>();
            result.forEach((k, v) -> map.put(Long.valueOf(k.toString()), v.toString()));
            return map;
        } catch (Exception e) {
            log.warn("Failed to fetch nicknames from auth service: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void attachNicknames(List<ReviewResponse> responses) {
        Map<Long, String> nicknames = fetchNicknames(responses);
        responses.forEach(r -> r.setNickname(nicknames.getOrDefault(r.getUserId(), "익명")));
    }

    public PageResponse<ReviewResponse> getReviews(Long menuId, Integer rating, int page, int size, String sort) {
        int offset = page * size;
        String orderBy = parseSortParam(sort);
        List<Review> reviews;
        long totalCount;

        if (menuId != null && rating != null) {
            reviews = reviewRepository.findByMenuIdAndRating(menuId, rating, offset, size, orderBy);
            totalCount = reviewRepository.countByMenuIdAndRating(menuId, rating);
        } else if (menuId != null) {
            reviews = reviewRepository.findByMenuId(menuId, offset, size, orderBy);
            totalCount = reviewRepository.countByMenuId(menuId);
        } else {
            reviews = reviewRepository.findAll(offset, size, orderBy);
            totalCount = reviewRepository.countAll();
        }

        List<ReviewResponse> content = reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
        attachNicknames(content);
        return PageResponse.of(content, page, size, totalCount, sort);
    }

    public PageResponse<ReviewResponse> getReviewsByOrder(Long orderId, int page, int size, String sort) {
        int offset = page * size;
        String orderBy = parseSortParam(sort);
        List<Review> reviews = reviewRepository.findByOrderId(orderId, offset, size, orderBy);
        long totalCount = reviewRepository.countByOrderId(orderId);
        List<ReviewResponse> content = reviews.stream().map(ReviewResponse::from).collect(Collectors.toList());
        attachNicknames(content);
        return PageResponse.of(content, page, size, totalCount, sort);
    }

    public PageResponse<ReviewResponse> getReviewsByUser(Long userId, int page, int size, String sort) {
        int offset = page * size;
        String orderBy = parseSortParam(sort);
        List<Review> reviews = reviewRepository.findByUserId(userId, offset, size, orderBy);
        long totalCount = reviewRepository.countByUserId(userId);
        List<ReviewResponse> content = reviews.stream().map(ReviewResponse::from).collect(Collectors.toList());
        attachNicknames(content);
        return PageResponse.of(content, page, size, totalCount, sort);
    }

    private String parseSortParam(String sort) {
        if (sort == null || sort.isBlank()) return "created_at DESC";
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toUpperCase() : "DESC";
        String column = switch (field) {
            case "rating" -> "rating";
            case "createdAt" -> "created_at";
            default -> "created_at";
        };
        if (!"ASC".equals(direction) && !"DESC".equals(direction)) direction = "DESC";
        return column + " " + direction;
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAiReviewSummary(Long menuId) {
        long reviewCount = reviewRepository.countByMenuId(menuId);
        String cacheKey = CACHE_PREFIX + menuId;

        // Check Redis cache
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            String[] parts = cached.split("\n", 2);
            long cachedCount = Long.parseLong(parts[0]);
            if (cachedCount == reviewCount) {
                return Map.of("summary", parts.length > 1 ? parts[1] : "", "count", reviewCount);
            }
        }

        List<Review> reviews = reviewRepository.findRecentByMenuId(menuId, 50);
        if (reviews.size() < 3) {
            return Map.of("summary", "", "count", reviews.size());
        }
        try {
            String menuName = reviews.get(0).getMenuName();
            List<Map<String, Object>> reviewData = reviews.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("text", r.getContent());
                m.put("rating", r.getRating());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> req = new HashMap<>();
            req.put("menu_id", menuId);
            req.put("menu_name", menuName);
            req.put("reviews", reviewData);

            Map<String, Object> res = restTemplate.postForObject(
                    aiReviewWriterUrl + "/reviews/summarize", req, Map.class);
            String summary = res != null ? (String) res.getOrDefault("summary_text", "") : "";

            // Cache to Redis (reviewCount + summary, 30min TTL)
            redisTemplate.opsForValue().set(cacheKey, reviewCount + "\n" + summary, CACHE_TTL);

            return Map.of("summary", summary, "count", reviews.size());
        } catch (Exception e) {
            log.warn("AI review summary failed: {}", e.getMessage());
            return Map.of("summary", "", "count", reviews.size());
        }
    }

    public AiGenerateResponse generateAiReview(AiGenerateRequest request) {
        try {
            // step 1: input validation skipped — negative reviews are valid

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

            // step 3: validate output (skip on failure)
            try {
                Map<String, Object> validationOutput = new HashMap<>();
                validationOutput.put("text", draft);
                validationOutput.put("type", "review_generate");
                validationOutput.put("context", request.getMenuName());
                Map<String, Object> outputValidation = restTemplate.postForObject(
                        aiValidationUrl + "/validation/output", validationOutput, Map.class);
                if (outputValidation != null && Boolean.FALSE.equals(outputValidation.get("is_valid"))) {
                    return new AiGenerateResponse("맛있게 잘 먹었습니다.", request.getRating());
                }
            } catch (Exception e) {
                log.warn("Output validation unavailable, skipping: {}", e.getMessage());
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
