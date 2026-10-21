package com.deliverytest.api.order.dto;

import com.deliverytest.api.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderResponse(
        String id,
        String customerName,
        String deliveryAddress,
        OrderStatus status,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
