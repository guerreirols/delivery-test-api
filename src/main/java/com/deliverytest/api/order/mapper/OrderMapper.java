package com.deliverytest.api.order.mapper;

import com.deliverytest.api.order.Order;
import com.deliverytest.api.order.dto.OrderRequest;
import com.deliverytest.api.order.dto.OrderResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@AllArgsConstructor
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .deliveryAddress(order.getDeliveryAddress())
                .status(order.getStatus())
                .items(order.getItems().stream().map(orderItemMapper::toResponse).toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public Order toEntity(OrderRequest orderRequest) {
        Order order = Order.builder()
                .customerName(orderRequest.customerName())
                .deliveryAddress(orderRequest.deliveryAddress())
                .items(new ArrayList<>())
                .build();
        orderRequest.items().stream()
                .map(orderItemMapper::toEntity)
                .forEach(order::addItem);
        return order;
    }
}
