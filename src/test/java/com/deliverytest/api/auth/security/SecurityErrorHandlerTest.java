package com.deliverytest.api.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {
    private final StaticMessageSource messages = new StaticMessageSource();
    private final ObjectMapper mapper = new ObjectMapper();
    private final SecurityErrorHandler handler = new SecurityErrorHandler(messages, mapper);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        request.addPreferredLocale(Locale.ENGLISH);
        messages.addMessage("error.auth.unauthorized", Locale.ENGLISH, "Authentication required");
        messages.addMessage("error.auth.forbidden", Locale.ENGLISH, "Access denied");
    }

    @Test
    void unauthorizedResponseIncludesBearerChallengeAndSafeJson() throws Exception {
        handler.commence(request, response, new BadCredentialsException("sensitive diagnostic"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(mapper.readTree(response.getContentAsString()).get("message").asText())
                .isEqualTo("Authentication required");
        assertThat(response.getContentAsString()).doesNotContain("sensitive diagnostic");
    }

    @Test
    void forbiddenResponseUsesRequestLocaleAndStandardErrorShape() throws Exception {
        handler.handle(request, response, new AccessDeniedException("internal detail"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(mapper.readTree(response.getContentAsString()).get("status").asInt()).isEqualTo(403);
        assertThat(mapper.readTree(response.getContentAsString()).get("message").asText()).isEqualTo("Access denied");
        assertThat(response.getContentAsString()).doesNotContain("internal detail");
    }
}
