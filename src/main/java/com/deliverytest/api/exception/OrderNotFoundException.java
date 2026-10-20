package com.deliverytest.api.exception;

public class OrderNotFoundException extends RuntimeException {

    private final String id;

    public OrderNotFoundException(String id) {
        super("error.order.not-found");
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
