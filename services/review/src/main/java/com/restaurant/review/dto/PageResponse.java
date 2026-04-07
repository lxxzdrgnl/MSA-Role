package com.restaurant.review.dto;

import java.util.List;

public class PageResponse<T> {

    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalCount;

    public PageResponse() {}

    public PageResponse(List<T> content, int currentPage, int totalPages, long totalCount) {
        this.content = content;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalCount = totalCount;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
}
