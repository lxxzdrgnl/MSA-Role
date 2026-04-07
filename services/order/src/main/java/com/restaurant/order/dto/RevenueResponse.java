package com.restaurant.order.dto;

public class RevenueResponse {

    private String period;
    private Integer totalRevenue;
    private Integer orderCount;

    public RevenueResponse() {}

    public RevenueResponse(String period, Integer totalRevenue, Integer orderCount) {
        this.period = period;
        this.totalRevenue = totalRevenue;
        this.orderCount = orderCount;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Integer totalRevenue) { this.totalRevenue = totalRevenue; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
}
