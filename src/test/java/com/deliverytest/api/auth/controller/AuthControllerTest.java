package com.deliverytest.api.auth.controller;

import com.deliverytest.api.auth.dto.AuthResponse;
import com.deliverytest.api.auth.dto.LoginRequest;
import com.deliverytest.api.auth.dto.SessionTokens;
import com.deliverytest.api.auth.security.RefreshCookie;
import com.deliverytest.api.auth.service.AuthService;
import com.deliverytest.api.auth.service.RefreshTokenService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    private final AuthService auth = mock(AuthService.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final AuthController controller = new AuthController(auth, refresh,
            new RefreshCookie(Duration.ofDays(7), true, "Lax"));
    private final AuthResponse access = new AuthResponse("access-token", "Bearer", 3600);

    @Test
    void loginReturnsAccessBodyAndRefreshCookieWithNoStore() {
        LoginRequest request = new LoginRequest("maria@example.com", "secret123");
        when(auth.login(request)).thenReturn(new SessionTokens(access, "refresh-token"));

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(access);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refresh_token=refresh-token", "HttpOnly");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void refreshReplacesCookieAndReturnsOnlyAccessResponse() {
        when(refresh.refresh("old-token")).thenReturn(new SessionTokens(access, "new-token"));

        ResponseEntity<AuthResponse> response = controller.refresh("old-token");

        assertThat(response.getBody()).isSameAs(access);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refresh_token=new-token");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void logoutRevokesTokenAndExpiresCookieWithoutResponseBody() {
        ResponseEntity<Void> response = controller.logout("refresh-token");

        verify(refresh).logout("refresh-token");
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0", "Path=/auth");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}
