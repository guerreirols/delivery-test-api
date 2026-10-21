package com.deliverytest.api.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class RefreshRequestFilter extends OncePerRequestFilter {
    private final BrowserAuthPolicy policy;
    private final SecurityErrorHandler errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (isProtectedRequest(request) && !isAllowed(request)) {
            errors.handle(request, response, new AccessDeniedException("Invalid browser authentication request"));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isProtectedRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST")
                && Set.of("/auth/login", "/auth/refresh", "/auth/logout").contains(path(request));
    }

    private boolean isAllowed(HttpServletRequest request) {
        if (CorsUtils.isCorsRequest(request) && !policy.allows(request.getHeader("Origin"))) {
            return false;
        }
        return path(request).equals("/auth/login")
                || "true".equals(request.getHeader("X-Refresh-Request"));
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
