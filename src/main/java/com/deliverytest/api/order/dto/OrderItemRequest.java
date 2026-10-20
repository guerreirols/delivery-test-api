package com.deliverytest.api.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank(message = "{validation.order-item.name.required}") String name,
        @NotNull(message = "{validation.order-item.quantity.required}")
        @Positive(message = "{validation.order-item.quantity.positive}") Integer quantity,
        @NotNull(message = "{validation.order-item.unit-price.required}")
        @Positive(message = "{validation.order-item.unit-price.positive}") BigDecimal unitPrice
) { }
