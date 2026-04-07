package com.restaurant.order.entity;

public class OrderItem {

    private Long id;
    private Long orderId;
    private Long menuId;
    private String menuName;
    private Integer price;
    private Integer quantity;

    public OrderItem() {}

    public OrderItem(Long orderId, Long menuId, String menuName, Integer price, Integer quantity) {
        this.orderId = orderId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.price = price;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getMenuId() { return menuId; }
    public void setMenuId(Long menuId) { this.menuId = menuId; }

    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
