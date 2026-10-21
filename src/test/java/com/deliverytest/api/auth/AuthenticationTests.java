package com.deliverytest.api.auth;

import com.deliverytest.api.auth.repository.RefreshTokenRepository;
import com.deliverytest.api.auth.service.AccessTokenService;
import com.deliverytest.api.user.entity.User;
import com.deliverytest.api.user.repository.UserRepository;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper mapper;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;

    @BeforeEach
    void clearUsers() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void sameOriginSwaggerCanRegisterAndLogin() throws Exception {
        mvc.perform(post("/auth/register").header("Origin", "http://localhost:8080")
                        .with(request -> {
                            request.setServerPort(8080);
                            return request;
                        }).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Swagger User","email":"swagger@example.com","password":"12345678"}
                        """))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/login").header("Origin", "http://localhost:8080")
                        .with(request -> {
                            request.setServerPort(8080);
                            return request;
                        }).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"swagger@example.com","password":"12345678"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() throws Exception {
        register("Maria@Example.com");
        User user = users.findByEmail("maria@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Maria");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getPassword()).isNotEqualTo("securePass123");
        assertThat(passwords.matches("securePass123", user.getPassword())).isTrue();
    }

    @Test
    void duplicateEmailReturnsConflictWithoutCreatingAnotherUser() throws Exception {
        register("maria@example.com");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registration("MARIA@example.com")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void invalidRegistrationReportsFieldsAndDoesNotPersist() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray());
        assertThat(users.count()).isZero();
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void loginTokenGrantsAccessWithoutSessionOrSensitiveClaims() throws Exception {
        register("maria@example.com");
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"MARIA@example.com\",\"password\":\"securePass123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600)).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        String token = mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(decoder.decode(token).getClaims()).containsOnlyKeys("sub", "iss", "iat", "exp");
        assertThat(decoder.decode(token).getSubject()).isEqualTo(users.findByEmail("maria@example.com").orElseThrow().getId());
        mvc.perform(get("/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"name\":\"Maria\"}"));
    }

    @Test
    void missingProfileUserReturnsSessionError() throws Exception {
        mvc.perform(get("/users/me").header("Authorization", "Bearer "
                        + token(Instant.now().plusSeconds(3600), AccessTokenService.ISSUER)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().json("""
                        {"status":401,"message":"Autenticação necessária ou token inválido"}
                        """));
    }

    @Test
    void disabledProfileUserReturnsSessionError() throws Exception {
        users.save(User.builder().id("user-id").name("Disabled").email("disabled@example.com")
                .password("unused").active(false).build());

        mvc.perform(get("/users/me").header("Authorization", "Bearer "
                        + token(Instant.now().plusSeconds(3600), AccessTokenService.ISSUER)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Autenticação necessária ou token inválido"));
    }

    @Test
    void wrongPasswordAndUnknownUserHaveSamePublicError() throws Exception {
        register("maria@example.com");
        String knownError = loginFailure("maria@example.com");
        assertThat(loginFailure("unknown@example.com")).isEqualTo(knownError);
    }

    @Test
    void anonymousCannotReadOrWriteOrdersOrAccessOtherRoutes() throws Exception {
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/orders")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/register")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidExpiredAndWrongIssuerTokensAreRejected() throws Exception {
        for (String token : new String[]{"invalid", token(Instant.now().minusSeconds(120), AccessTokenService.ISSUER),
                token(Instant.now().plusSeconds(3600), "untrusted"), signedWithDifferentKey()}) {
            mvc.perform(get("/orders").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Autenticação necessária ou token inválido"));
        }
    }

    private String signedWithDifferentKey() {
        NimbusJwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[32], "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(AccessTokenService.ISSUER).subject("user-id")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
        return otherEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String token(Instant expiry, String issuer) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject("user-id")
                .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(expiry).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String loginFailure(String email) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"))
                .andReturn().getResponse().getContentAsString();
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration(email)))
                .andExpect(status().isCreated()).andExpect(content().string(""));
    }

    private String registration(String email) {
        return "{\"name\":\" Maria \",\"email\":\"" + email + "\",\"password\":\"securePass123\"}";
    }
}
