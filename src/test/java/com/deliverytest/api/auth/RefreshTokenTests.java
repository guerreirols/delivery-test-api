package com.deliverytest.api.auth;

import com.deliverytest.api.auth.entity.RefreshToken;
import com.deliverytest.api.auth.repository.RefreshTokenRepository;
import com.deliverytest.api.auth.security.RefreshCookie;
import com.deliverytest.api.auth.service.AccessTokenService;
import com.deliverytest.api.auth.service.RefreshTokenService;
import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenTests {
    private static final String ORIGIN = "http://localhost:5173";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired ObjectMapper mapper;
    @Autowired RefreshTokenService refreshService;
    @MockitoSpyBean AccessTokenService accessTokens;

    @BeforeEach
    void prepareUser() throws Exception {
        refreshTokens.deleteAll();
        users.deleteAll();
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Maria\",\"email\":\"maria@example.com\",\"password\":\"securePass123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void loginStoresOnlyHashAndSetsRestrictedCookie() throws Exception {
        MvcResult login = login();
        Cookie cookie = refreshCookie(login);
        assertThat(cookie.getValue()).hasSize(43);
        assertThat(login.getResponse().getHeader("Set-Cookie"))
                .contains("HttpOnly", "Secure", "Path=/auth", "SameSite=Lax", "Max-Age=604800");
        assertThat(login.getResponse().getContentAsString()).doesNotContain(cookie.getValue(), "refreshToken", "password");
        assertThat(refreshTokens.findAll()).hasSize(1);
        assertThat(refreshTokens.findAll().getFirst().getTokenHash()).isEqualTo(hash(cookie.getValue()));
    }

    @Test
    void refreshRotatesCookieAndRejectsReplayWhileNewAccessWorks() throws Exception {
        Cookie original = refreshCookie(login());
        MvcResult refreshed = refresh(original);
        Cookie replacement = refreshCookie(refreshed);
        assertThat(replacement.getValue()).isNotEqualTo(original.getValue());
        assertThat(refreshTokens.existsByTokenHash(hash(original.getValue()))).isFalse();
        assertThat(refreshTokens.existsByTokenHash(hash(replacement.getValue()))).isTrue();
        mvc.perform(post("/auth/refresh").header("X-Refresh-Request", "true").cookie(original))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        String access = mapper.readTree(refreshed.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/orders").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        refresh(replacement);
    }

    @Test
    void expiredCookieCannotRefresh() throws Exception {
        Cookie cookie = refreshCookie(login());
        RefreshToken existing = refreshTokens.findByTokenHash(hash(cookie.getValue())).orElseThrow();
        refreshTokens.delete(existing);
        refreshTokens.save(new RefreshToken(existing.getTokenHash(), existing.getUser(), Instant.now().minusSeconds(1)));
        mvc.perform(post("/auth/refresh").header("X-Refresh-Request", "true").cookie(cookie))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Refresh token inválido ou expirado"));
    }

    @Test
    void inactiveUserCannotRefresh() throws Exception {
        Cookie cookie = refreshCookie(login());
        com.deliverytest.api.user.entity.User user = users.findByEmail("maria@example.com").orElseThrow();
        users.save(com.deliverytest.api.user.entity.User.builder().id(user.getId()).name(user.getName())
                .email(user.getEmail()).password(user.getPassword()).createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt()).active(false).build());
        mvc.perform(post("/auth/refresh").header("X-Refresh-Request", "true").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesCookieAndIsIdempotent() throws Exception {
        Cookie cookie = refreshCookie(login());
        mvc.perform(post("/auth/logout").header("X-Refresh-Request", "true").cookie(cookie))
                .andExpect(status().isNoContent()).andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
        assertThat(refreshTokens.count()).isZero();
        mvc.perform(post("/auth/refresh").header("X-Refresh-Request", "true").cookie(cookie))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").header("X-Refresh-Request", "true"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/auth/logout").header("X-Refresh-Request", "true")
                        .cookie(new Cookie(RefreshCookie.NAME, "invalid")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void absentOrMalformedRefreshCookieReturnsUnauthorized() throws Exception {
        mvc.perform(post("/auth/refresh").header("X-Refresh-Request", "true"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").header("X-Refresh-Request", "true")
                        .cookie(new Cookie(RefreshCookie.NAME, "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshAndLogoutRejectRequestsWithoutCustomHeader() throws Exception {
        Cookie cookie = refreshCookie(login());
        for (String endpoint : new String[]{"/auth/refresh", "/auth/logout"}) {
            mvc.perform(post(endpoint).cookie(cookie)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
        assertThat(refreshTokens.count()).isEqualTo(1);
    }

    @Test
    void hostileOriginCannotLoginRefreshOrLogout() throws Exception {
        Cookie cookie = refreshCookie(login());
        for (String endpoint : new String[]{"/auth/login", "/auth/refresh", "/auth/logout"}) {
            mvc.perform(post(endpoint).header("Origin", "https://hostile.example")
                            .header("X-Refresh-Request", "true").cookie(cookie))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        }
        assertThat(refreshTokens.count()).isEqualTo(1);
    }

    @Test
    void corsAllowsCredentialedReactPreflightOnlyForConfiguredOrigin() throws Exception {
        mvc.perform(options("/auth/refresh").header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Refresh-Request"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/auth/refresh").header("Origin", "https://hostile.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void failedAccessTokenIssuanceRollsBackRefreshRotation() throws Exception {
        Cookie cookie = refreshCookie(login());
        IllegalStateException failure = new IllegalStateException("Token signing unavailable");
        doThrow(failure).when(accessTokens).issue(anyString());
        assertThatThrownBy(() -> refreshService.refresh(cookie.getValue())).isSameAs(failure);
        assertThat(refreshTokens.existsByTokenHash(hash(cookie.getValue()))).isTrue();
        assertThat(refreshTokens.count()).isEqualTo(1);
        reset(accessTokens);
        refresh(cookie);
    }

    @Test
    void failedLoginTokenIssuanceDoesNotPersistRefreshSession() {
        IllegalStateException failure = new IllegalStateException("Token signing unavailable");
        doThrow(failure).when(accessTokens).issue(anyString());
        assertThatThrownBy(() -> refreshService.create(users.findByEmail("maria@example.com").orElseThrow()))
                .isSameAs(failure);
        assertThat(refreshTokens.count()).isZero();
    }

    private MvcResult login() throws Exception {
        return mvc.perform(post("/auth/login").header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@example.com\",\"password\":\"securePass123\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
    }

    private MvcResult refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/auth/refresh").header("Origin", ORIGIN)
                        .header("X-Refresh-Request", "true").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN)).andReturn();
    }

    private Cookie refreshCookie(MvcResult result) {
        return result.getResponse().getCookie(RefreshCookie.NAME);
    }

    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.US_ASCII)));
    }
}
