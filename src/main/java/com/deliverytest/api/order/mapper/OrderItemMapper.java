package com.deliverytest.api.order.mapper;

import com.deliverytest.api.order.OrderItem;
import com.deliverytest.api.order.dto.OrderItemRequest;
import com.deliverytest.api.order.dto.OrderItemResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItemResponse toResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .name(orderItem.getName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .build();
    }

    public OrderItem toEntity(OrderItemRequest orderItemRequest) {
        return OrderItem.builder()
                .name(orderItemRequest.name())
                .quantity(orderItemRequest.quantity())
                .unitPrice(orderItemRequest.unitPrice())
                .build();
    }
}
