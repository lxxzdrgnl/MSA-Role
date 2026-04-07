package com.restaurant.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReviewCreateRequest {

    @NotNull(message = "order_id is required")
    private Long orderId;

    @NotNull(message = "menu_id is required")
    private Long menuId;

    @NotBlank(message = "menu_name is required")
    private String menuName;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @NotBlank(message = "content is required")
    private String content;

    private Integer isAiGenerated;

    public ReviewCreateRequest() {}

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
}
