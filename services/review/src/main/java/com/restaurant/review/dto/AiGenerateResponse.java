package com.restaurant.review.dto;

public class AiGenerateResponse {

    private String draft;
    private Integer rating;

    public AiGenerateResponse() {}

    public AiGenerateResponse(String draft, Integer rating) {
        this.draft = draft;
        this.rating = rating;
    }

    public String getDraft() { return draft; }
    public void setDraft(String draft) { this.draft = draft; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
