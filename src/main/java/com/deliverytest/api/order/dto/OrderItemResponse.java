package com.deliverytest.api.order.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record OrderItemResponse(
        String id,
        String name,
        Integer quantity,
        BigDecimal unitPrice
) { }
