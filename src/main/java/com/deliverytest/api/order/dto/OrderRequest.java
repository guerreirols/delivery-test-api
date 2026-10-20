package com.deliverytest.api.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "{validation.order.customer-name.required}") String customerName,
        @NotBlank(message = "{validation.order.delivery-address.required}") String deliveryAddress,
        @NotEmpty(message = "{validation.order.items.required}") List<@Valid OrderItemRequest> items
) { }
