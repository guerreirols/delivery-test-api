package com.deliverytest.api.exception;

import com.deliverytest.api.auth.exception.InvalidRefreshTokenException;
import com.deliverytest.api.order.entity.OrderStatus;
import com.deliverytest.api.order.exception.InvalidOrderStatusException;
import com.deliverytest.api.order.exception.OrderNotFoundException;
import com.deliverytest.api.user.exception.EmailAlreadyRegisteredException;
import com.deliverytest.api.user.exception.InvalidUserSessionException;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final StaticMessageSource messages = new StaticMessageSource();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages);

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        messages.addMessage("error.order.not-found", Locale.ENGLISH, "Order {0} not found");
        messages.addMessage("error.order.invalid-status-transition", Locale.ENGLISH, "Cannot change {0} to {1}");
        messages.addMessage("error.auth.email-already-registered", Locale.ENGLISH, "Email already registered");
        messages.addMessage("error.auth.invalid-credentials", Locale.ENGLISH, "Invalid credentials");
        messages.addMessage("error.auth.unauthorized", Locale.ENGLISH, "Authentication required or invalid token");
        messages.addMessage("error.auth.invalid-refresh", Locale.ENGLISH, "Invalid refresh");
        messages.addMessage("error.validation", Locale.ENGLISH, "Invalid fields");
        messages.addMessage("error.request.malformed", Locale.ENGLISH, "Malformed request");
        messages.addMessage("error.internal", Locale.ENGLISH, "Operation unavailable");
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void infrastructureAuthenticationFailureIsNotReportedAsWrongPassword() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationService(
                new AuthenticationServiceException("database unavailable"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(500, "Operation unavailable", null));
    }

    @Test
    void missingOrderReturns404WithIdentifierInLocalizedMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new OrderNotFoundException("missing-id"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(404, "Order missing-id not found", null));
    }

    @Test
    void invalidTransitionReturns409WithBothStatuses() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidStatus(
                new InvalidOrderStatusException(OrderStatus.RECEBIDO, OrderStatus.ENTREGUE));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Cannot change RECEBIDO to ENTREGUE");
    }

    @Test
    void duplicateEmailReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmail(new EmailAlreadyRegisteredException());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(409, "Email already registered", null));
    }

    @Test
    void invalidUserSessionReturnsUnauthorizedWithoutLoginMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidUserSession(new InvalidUserSessionException());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getFirst("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(401, "Authentication required or invalid token", null));
    }

    @Test
    void invalidCredentialsDoNotExposeInternalExceptionMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(new BadCredentialsException("internal detail"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(401, "Invalid credentials", null));
    }

    @Test
    void invalidRefreshReturnsUnauthorized() {
        InvalidRefreshTokenException failure = new InvalidRefreshTokenException();
        messages.addMessage(failure.getMessage(), Locale.ENGLISH, "Invalid refresh");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRefresh(failure);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(401, "Invalid refresh", null));
    }

    @Test
    void malformedRequestReturnsSafe400Message() {
        assertThat(handler.handleUnreadable().getBody()).isEqualTo(new ErrorResponse(400, "Malformed request", null));
    }

    @Test
    void validationReturnsFieldsWithoutRejectedValues() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "password", "sensitive", false, null, null, "Password too short"));
        MethodArgumentNotValidException failure = mock(MethodArgumentNotValidException.class);
        when(failure.getBindingResult()).thenReturn(binding);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(failure);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Invalid fields");
        assertThat(response.getBody().fieldErrors()).containsExactly(new ErrorResponse.FieldError("password", "Password too short"));
    }

    @Test
    void resolvesMessageUsingCurrentRequestLocale() {
        Locale portuguese = Locale.forLanguageTag("pt-BR");
        messages.addMessage("error.order.not-found", portuguese, "Pedido {0} não encontrado");
        LocaleContextHolder.setLocale(portuguese);

        assertThat(handler.handleNotFound(new OrderNotFoundException("123")).getBody().message())
                .isEqualTo("Pedido 123 não encontrado");
    }
}
