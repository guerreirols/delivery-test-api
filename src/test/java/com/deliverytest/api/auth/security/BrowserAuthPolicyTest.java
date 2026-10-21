package com.deliverytest.api.auth.security;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrowserAuthPolicyTest {
    @Test
    void allowsOnlyExactConfiguredOriginsAndNonBrowserClients() {
        BrowserAuthPolicy policy = new BrowserAuthPolicy(List.of("http://localhost:5173"));

        assertThat(policy.allows("http://localhost:5173")).isTrue();
        assertThat(policy.allows(null)).isTrue();
        assertThat(policy.allows("http://localhost:5173.evil.example")).isFalse();
        assertThat(policy.allows("http://localhost:3000")).isFalse();
        assertThat(policy.allows("null")).isFalse();
    }

    @Test
    void configurationCannotBeChangedThroughOriginalList() {
        List<String> origins = new ArrayList<>(List.of("http://localhost:5173"));
        BrowserAuthPolicy policy = new BrowserAuthPolicy(origins);

        origins.add("https://hostile.example");

        assertThat(policy.allows("https://hostile.example")).isFalse();
        assertThatThrownBy(() -> policy.allowedOrigins().add("https://hostile.example"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsEmptyOrigins() {
        assertThatThrownBy(() -> new BrowserAuthPolicy(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.example.com"})
    void rejectsWildcardOrigins(String origin) {
        assertThatThrownBy(() -> new BrowserAuthPolicy(List.of(origin))).isInstanceOf(IllegalArgumentException.class);
    }
}
