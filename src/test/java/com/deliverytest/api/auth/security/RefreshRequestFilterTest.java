package com.deliverytest.api.auth.security;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RefreshRequestFilterTest {
    private final SecurityErrorHandler errors = mock(SecurityErrorHandler.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final RefreshRequestFilter filter = new RefreshRequestFilter(
            new BrowserAuthPolicy(List.of("http://localhost:5173")), errors);

    @ParameterizedTest
    @MethodSource("requests")
    void guardsBrowserAuthenticationWithoutBlockingOtherRequests(String method, String path,
                                                                String origin, String header, boolean allowed) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api" + path);
        request.setContextPath("/api");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        if (header != null) {
            request.addHeader("X-Refresh-Request", header);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        if (allowed) {
            verify(chain).doFilter(request, response);
            verifyNoInteractions(errors);
        } else {
            verify(errors).handle(eq(request), eq(response), any(AccessDeniedException.class));
            verifyNoInteractions(chain);
        }
    }

    static Stream<Arguments> requests() {
        return Stream.of(
                Arguments.of("POST", "/auth/refresh", "http://localhost:5173", "true", true),
                Arguments.of("POST", "/auth/logout", null, "true", true),
                Arguments.of("POST", "/auth/refresh", null, null, false),
                Arguments.of("POST", "/auth/logout", "http://localhost:5173", "false", false),
                Arguments.of("POST", "/auth/refresh", "https://hostile.example", "true", false),
                Arguments.of("POST", "/auth/login", "https://hostile.example", null, false),
                Arguments.of("POST", "/auth/login", "http://localhost:5173", null, true),
                Arguments.of("POST", "/auth/login", "http://localhost", null, true),
                Arguments.of("POST", "/auth/login", "http://localhost:80", null, true),
                Arguments.of("POST", "/auth/login", "http://localhost:8080", null, false),
                Arguments.of("POST", "/auth/refresh", "http://localhost", "true", true),
                Arguments.of("POST", "/auth/refresh", "http://localhost", null, false),
                Arguments.of("POST", "/auth/logout", "http://localhost", "true", true),
                Arguments.of("POST", "/auth/login", null, null, true),
                Arguments.of("OPTIONS", "/auth/refresh", "http://localhost:5173", null, true),
                Arguments.of("GET", "/orders", null, null, true));
    }
}
