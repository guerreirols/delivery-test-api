package com.deliverytest.api.exception;

import com.deliverytest.api.auth.exception.InvalidRefreshTokenException;
import com.deliverytest.api.order.exception.InvalidOrderStatusException;
import com.deliverytest.api.order.exception.OrderNotFoundException;
import com.deliverytest.api.user.exception.EmailAlreadyRegisteredException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        log.debug("Renovação de sessão rejeitada: token inválido ou sessão indisponível");
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(EmailAlreadyRegisteredException ex) {
        log.debug("Cadastro rejeitado: e-mail já cadastrado");
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.debug("Autenticação rejeitada: tipo={}", ex.getClass().getSimpleName());
        return build(HttpStatus.UNAUTHORIZED, "error.auth.invalid-credentials");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
        log.debug("Consulta rejeitada: pedido não encontrado");
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getId());
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatus(InvalidOrderStatusException ex) {
        log.debug("Transição de pedido rejeitada: de={}, para={}", ex.getFrom(), ex.getTo());
        return build(HttpStatus.CONFLICT, ex.getMessage(), ex.getFrom(), ex.getTo());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.debug("Requisição rejeitada: validação falhou em {} campo(s)", ex.getBindingResult().getFieldErrorCount());
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), message("error.validation"), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable() {
        log.debug("Requisição rejeitada: corpo inválido ou mal formatado");
        return build(HttpStatus.BAD_REQUEST, "error.request.malformed");
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationService(AuthenticationServiceException exception) {
        return internalError(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) throws Exception {
        if (exception instanceof org.springframework.web.ErrorResponse
                || exception instanceof TypeMismatchException
                || exception instanceof HttpMessageConversionException) {
            throw exception;
        }
        return internalError(exception);
    }

    private ResponseEntity<ErrorResponse> internalError(Exception exception) {
        Throwable diagnostic = diagnostic(exception);
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        if (cause != exception) {
            diagnostic.initCause(diagnostic(cause));
        }
        log.error("Falha inesperada no processamento da requisição", diagnostic);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");
    }

    private Throwable diagnostic(Throwable exception) {
        Throwable diagnostic = new Throwable(exception.getClass().getName());
        diagnostic.setStackTrace(exception.getStackTrace());
        return diagnostic;
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String key, Object... args) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message(key, args), null));
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
