package com.restaurant.review.dto;

import java.util.Map;

public class ReviewSummaryResponse {

    private double averageRating;
    private long totalCount;
    private Map<Integer, Long> distribution;

    public ReviewSummaryResponse() {}

    public ReviewSummaryResponse(double averageRating, long totalCount, Map<Integer, Long> distribution) {
        this.averageRating = averageRating;
        this.totalCount = totalCount;
        this.distribution = distribution;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public Map<Integer, Long> getDistribution() { return distribution; }
    public void setDistribution(Map<Integer, Long> distribution) { this.distribution = distribution; }
}
