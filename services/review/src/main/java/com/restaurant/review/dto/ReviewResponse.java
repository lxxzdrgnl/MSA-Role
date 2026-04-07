package com.restaurant.review.dto;

import com.restaurant.review.entity.Review;

import java.time.LocalDateTime;

public class ReviewResponse {

    private Long id;
    private Long userId;
    private Long orderId;
    private Long menuId;
    private String menuName;
    private Integer rating;
    private String content;
    private boolean aiGenerated;
    private LocalDateTime createdAt;

    public ReviewResponse() {}

    public static ReviewResponse from(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setOrderId(review.getOrderId());
        response.setMenuId(review.getMenuId());
        response.setMenuName(review.getMenuName());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setAiGenerated(review.getIsAiGenerated() != null && review.getIsAiGenerated() == 1);
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }

    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
