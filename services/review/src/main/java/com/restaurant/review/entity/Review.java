package com.restaurant.review.entity;

import java.time.LocalDateTime;

public class Review {

    private Long id;
    private Long userId;
    private Long orderId;
    private Long menuId;
    private String menuName;
    private Integer rating;
    private String content;
    private Integer isAiGenerated;
    private String adminReply;
    private LocalDateTime adminReplyAt;
    private LocalDateTime createdAt;

    public Review() {}

    public Review(Long userId, Long orderId, Long menuId, String menuName, Integer rating, String content, Integer isAiGenerated) {
        this.userId = userId;
        this.orderId = orderId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.rating = rating;
        this.content = content;
        this.isAiGenerated = isAiGenerated;
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

    public Integer getIsAiGenerated() { return isAiGenerated; }
    public void setIsAiGenerated(Integer isAiGenerated) { this.isAiGenerated = isAiGenerated; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

    public LocalDateTime getAdminReplyAt() { return adminReplyAt; }
    public void setAdminReplyAt(LocalDateTime adminReplyAt) { this.adminReplyAt = adminReplyAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
