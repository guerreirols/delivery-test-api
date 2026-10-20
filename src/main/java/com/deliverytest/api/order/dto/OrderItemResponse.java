package com.deliverytest.api.order.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItemResponse(
        String id,
        String name,
        Integer quantity,
        BigDecimal unitPrice
) { }
