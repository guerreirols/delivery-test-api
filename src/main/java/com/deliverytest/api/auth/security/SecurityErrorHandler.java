package com.deliverytest.api.auth.security;

import com.deliverytest.api.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        log.debug("Acesso sem autenticação válida: tipo={}", exception.getClass().getSimpleName());
        response.setHeader("WWW-Authenticate", "Bearer");
        write(request, response, 401, "error.auth.unauthorized");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        log.debug("Acesso negado pela política de segurança: tipo={}", exception.getClass().getSimpleName());
        write(request, response, 403, "error.auth.forbidden");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       int status, String key) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String message = messageSource.getMessage(key, null, request.getLocale());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(status, message, null));
    }
}
