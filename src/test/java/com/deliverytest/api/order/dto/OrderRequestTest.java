package com.deliverytest.api.order.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRequestTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    @Test
    void acceptsValidOrderAndPositiveMonetaryValues() {
        OrderRequest request = new OrderRequest("Maria", "Rua A",
                List.of(new OrderItemRequest("Pizza", 1, new BigDecimal("0.01"))));

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidOrders")
    void rejectsInvalidOrderField(OrderRequest request, String field) {
        assertThat(VALIDATOR.validate(request)).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo(field));
    }

    @Test
    void validatesItemsRecursively() {
        OrderRequest request = new OrderRequest("Maria", "Rua A",
                List.of(new OrderItemRequest(" ", 0, BigDecimal.ZERO)));

        assertThat(VALIDATOR.validate(request)).hasSize(3)
                .allSatisfy(violation -> assertThat(violation.getPropertyPath().toString()).startsWith("items[0]"));
    }

    @Test
    void rejectsMissingStatus() {
        assertThat(VALIDATOR.validate(new OrderStatusUpdateRequest(null))).hasSize(1);
    }

    static Stream<Arguments> invalidOrders() {
        OrderItemRequest item = new OrderItemRequest("Pizza", 1, BigDecimal.ONE);
        return Stream.of(
                Arguments.of(new OrderRequest(" ", "Rua A", List.of(item)), "customerName"),
                Arguments.of(new OrderRequest("Maria", null, List.of(item)), "deliveryAddress"),
                Arguments.of(new OrderRequest("Maria", "Rua A", List.of()), "items"),
                Arguments.of(new OrderRequest("Maria", "Rua A", null), "items"));
    }
}
