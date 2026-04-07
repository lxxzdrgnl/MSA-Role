package com.restaurant.order.dto;

public class ActiveCountResponse {

    private int activeCount;
    private int pendingCount;
    private int cookingCount;

    public ActiveCountResponse() {}

    public ActiveCountResponse(int activeCount, int pendingCount, int cookingCount) {
        this.activeCount = activeCount;
        this.pendingCount = pendingCount;
        this.cookingCount = cookingCount;
    }

    public int getActiveCount() { return activeCount; }
    public void setActiveCount(int activeCount) { this.activeCount = activeCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getCookingCount() { return cookingCount; }
    public void setCookingCount(int cookingCount) { this.cookingCount = cookingCount; }
}
