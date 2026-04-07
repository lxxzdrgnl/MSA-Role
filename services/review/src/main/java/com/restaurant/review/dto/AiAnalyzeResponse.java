package com.restaurant.review.dto;

public class AiAnalyzeResponse {

    private String summary;
    private Object details;

    public AiAnalyzeResponse() {}

    public AiAnalyzeResponse(String summary, Object details) {
        this.summary = summary;
        this.details = details;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}
