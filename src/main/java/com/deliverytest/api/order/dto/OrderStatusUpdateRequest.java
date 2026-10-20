package com.deliverytest.api.order.dto;

import com.deliverytest.api.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "{validation.order-status.required}") OrderStatus status
) {}
