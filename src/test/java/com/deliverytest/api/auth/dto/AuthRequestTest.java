package com.deliverytest.api.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 64})
    void acceptsRegistrationPasswordAtBothBoundaries(int length) {
        assertThat(VALIDATOR.validate(new RegisterRequest("Maria", "maria@example.com", "a".repeat(length)))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidInputAtHttpBoundary(Object request, String field) {
        assertThat(VALIDATOR.validate(request)).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo(field));
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(new RegisterRequest(" ", "maria@example.com", "secret123"), "name"),
                Arguments.of(new RegisterRequest("x".repeat(121), "maria@example.com", "secret123"), "name"),
                Arguments.of(new RegisterRequest("Maria", "invalid", "secret123"), "email"),
                Arguments.of(new RegisterRequest("Maria", null, "secret123"), "email"),
                Arguments.of(new RegisterRequest("Maria", "maria@example.com", "1234567"), "password"),
                Arguments.of(new RegisterRequest("Maria", "maria@example.com", "x".repeat(65)), "password"),
                Arguments.of(new LoginRequest("", "secret123"), "email"),
                Arguments.of(new LoginRequest("invalid", "secret123"), "email"),
                Arguments.of(new LoginRequest("maria@example.com", " "), "password"),
                Arguments.of(new LoginRequest("maria@example.com", "x".repeat(65)), "password"));
    }
}
