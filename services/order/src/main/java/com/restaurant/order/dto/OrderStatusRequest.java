package com.restaurant.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OrderStatusRequest {

    @NotBlank(message = "status is required")
    @Pattern(regexp = "PENDING|COOKING|DONE", message = "status must be PENDING, COOKING, or DONE")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
