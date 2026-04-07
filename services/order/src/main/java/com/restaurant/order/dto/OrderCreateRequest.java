package com.restaurant.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderCreateRequest {

    @NotEmpty(message = "Order items must not be empty")
    @Valid
    private List<OrderItemRequest> items;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {

        @jakarta.validation.constraints.NotNull(message = "menuId is required")
        private Long menuId;

        @jakarta.validation.constraints.Min(value = 1, message = "quantity must be at least 1")
        private int quantity = 1;

        public Long getMenuId() { return menuId; }
        public void setMenuId(Long menuId) { this.menuId = menuId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
