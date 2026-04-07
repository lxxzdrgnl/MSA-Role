package com.restaurant.menu.entity;

import java.time.LocalDateTime;

public class Menu {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private Integer price;
    private String imageUrl;
    private String tags;
    private String allergens;
    private Integer spicyLevel;
    private Integer cookTimeMinutes;
    private Boolean isSoldOut;
    private Boolean isBest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Menu() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getAllergens() { return allergens; }
    public void setAllergens(String allergens) { this.allergens = allergens; }

    public Integer getSpicyLevel() { return spicyLevel; }
    public void setSpicyLevel(Integer spicyLevel) { this.spicyLevel = spicyLevel; }

    public Integer getCookTimeMinutes() { return cookTimeMinutes; }
    public void setCookTimeMinutes(Integer cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }

    public Boolean getIsSoldOut() { return isSoldOut; }
    public void setIsSoldOut(Boolean isSoldOut) { this.isSoldOut = isSoldOut; }

    public Boolean getIsBest() { return isBest; }
    public void setIsBest(Boolean isBest) { this.isBest = isBest; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
