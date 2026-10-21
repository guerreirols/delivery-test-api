package com.deliverytest.api.order.exception;

import com.deliverytest.api.order.entity.OrderStatus;
import lombok.Getter;

@Getter
public class InvalidOrderStatusException extends RuntimeException {

    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidOrderStatusException(OrderStatus from, OrderStatus to) {
        super("error.order.invalid-status-transition");
        this.from = from;
        this.to = to;
    }
}
