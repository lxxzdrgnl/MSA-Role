package com.restaurant.order.dto;

public class BestSellerResponse {

    private Long menuId;
    private String menuName;
    private Integer totalQuantity;
    private Integer totalRevenue;

    public BestSellerResponse() {}

    public BestSellerResponse(Long menuId, String menuName, Integer totalQuantity, Integer totalRevenue) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }

    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }

    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Integer totalRevenue) { this.totalRevenue = totalRevenue; }
}
