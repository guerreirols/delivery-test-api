package com.deliverytest.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) { }
}
