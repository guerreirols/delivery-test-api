package com.deliverytest.api.auth.security;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshCookieTest {
    @Test
    void issuesSecureHttpOnlyCookieWithRestrictedPathAndLifetime() {
        String cookie = new RefreshCookie(Duration.ofDays(7), true, "Lax").issue("opaque-token");

        assertThat(cookie).contains("refresh_token=opaque-token", "Path=/auth", "HttpOnly",
                "Secure", "SameSite=Lax", "Max-Age=604800");
        assertThat(cookie).doesNotContain("Domain=");
    }

    @Test
    void clearingCookieUsesSameScopeAndZeroLifetime() {
        String cookie = new RefreshCookie(Duration.ofDays(7), true, "None").clear();

        assertThat(cookie).contains("refresh_token=;", "Max-Age=0", "Path=/auth", "HttpOnly", "Secure", "SameSite=None");
    }

    @Test
    void supportsExplicitLocalHttpConfiguration() {
        String cookie = new RefreshCookie(Duration.ofDays(1), false, "Lax").issue("token");

        assertThat(cookie).contains("HttpOnly", "SameSite=Lax").doesNotContain("Secure");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveLifetime(long seconds) {
        assertThatThrownBy(() -> new RefreshCookie(Duration.ofSeconds(seconds), true, "Lax"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"false,None", "true,invalid", "false,invalid"})
    void rejectsUnsafeOrUnknownSameSiteConfiguration(boolean secure, String sameSite) {
        assertThatThrownBy(() -> new RefreshCookie(Duration.ofDays(7), secure, sameSite))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
